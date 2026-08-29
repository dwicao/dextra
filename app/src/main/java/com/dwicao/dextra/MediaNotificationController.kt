package com.dwicao.dextra

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.mozilla.geckoview.MediaSession as GeckoMediaSession

/** Bridges the active Gecko media session to Android lock-screen controls. */
class MediaNotificationController(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val mediaSession = MediaSessionCompat(appContext, "Dextra")
    private var target: Target? = null
    private var playing = false
    private var duration = 0L
    private var position = 0L

    init {
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                target?.session?.play()
                setPlaying(true)
            }

            override fun onPause() {
                target?.session?.pause()
                setPlaying(false)
            }

            override fun onStop() {
                target?.session?.stop()
                clear(target?.tabId)
            }

            override fun onSeekTo(pos: Long) {
                target?.session?.seekTo(pos / 1000.0, false)
                position = pos
                publish()
            }
        })
        mediaSession.isActive = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Media controls", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    fun activate(
        tabId: String,
        session: GeckoMediaSession,
        title: String,
        artist: String?,
        album: String?,
        privateTab: Boolean,
        targetActivity: Class<out android.app.Activity> = MainActivity::class.java,
    ) {
        if (privateTab) {
            clear(tabId)
            return
        }
        target = Target(tabId, session, title, artist, album, targetActivity)
        playing = true
        duration = 0L
        position = 0L
        updateMetadata(title, artist, album)
    }

    fun updateMetadata(tabId: String, title: String, artist: String?, album: String?) {
        if (target?.tabId != tabId) return
        target = target?.copy(title = title, artist = artist, album = album)
        publish()
    }

    fun updatePosition(tabId: String, durationSeconds: Double, positionSeconds: Double, playbackRate: Double) {
        if (target?.tabId != tabId) return
        duration = (durationSeconds * 1000).toLong().coerceAtLeast(0L)
        position = (positionSeconds * 1000).toLong().coerceIn(0L, duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    position,
                    playbackRate.toFloat().coerceAtLeast(0.1f),
                )
                .build(),
        )
        publish()
    }

    fun setPlaying(tabId: String, isPlaying: Boolean) {
        if (target?.tabId != tabId) return
        playing = isPlaying
        publish()
    }

    fun handleAction(action: String?) {
        when (action) {
            ACTION_PLAY -> {
                target?.session?.play()
                setPlaying(true)
            }
            ACTION_PAUSE -> {
                target?.session?.pause()
                setPlaying(false)
            }
            ACTION_STOP -> {
                target?.session?.stop()
                clear(target?.tabId)
            }
        }
    }

    fun clear(tabId: String?) {
        if (tabId == null || target?.tabId != tabId) return
        target = null
        playing = false
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun close() {
        target = null
        notificationManager.cancel(NOTIFICATION_ID)
        mediaSession.isActive = false
        mediaSession.release()
    }

    private fun setPlaying(isPlaying: Boolean) {
        playing = isPlaying
        publish()
    }

    private fun updateMetadata(title: String, artist: String?, album: String?) {
        target = target?.copy(title = title, artist = artist, album = album)
        publish()
    }

    private fun publish() {
        val current = target ?: return
        val state = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO)
            .setState(if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, position, 1f)
            .build()
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, current.title.ifBlank { "Dextra media" })
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, current.artist.orEmpty())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, current.album.orEmpty())
                .build(),
        )
        mediaSession.setPlaybackState(state)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val clickIntent = PendingIntent.getActivity(
            appContext,
            4101,
            Intent(appContext, current.targetActivity)
                .putExtra(EXTRA_MEDIA_TAB_ID, current.tabId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val playAction = if (playing) {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", mediaActionIntent(ACTION_PAUSE, current.tabId))
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", mediaActionIntent(ACTION_PLAY, current.tabId))
        }
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(current.title.ifBlank { "Dextra media" })
                .setContentText(current.artist.orEmpty().ifBlank { "Playing in Dextra" })
                .setContentIntent(clickIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(playAction)
                .addAction(NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", mediaActionIntent(ACTION_STOP, current.tabId)))
                .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0, 1))
                .build(),
        )
    }

    private fun mediaActionIntent(action: String, tabId: String): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        action.hashCode(),
        Intent(appContext, MediaNotificationReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_MEDIA_TAB_ID, tabId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private data class Target(
        val tabId: String,
        val session: GeckoMediaSession,
        val title: String = "Dextra media",
        val artist: String? = null,
        val album: String? = null,
        val targetActivity: Class<out android.app.Activity> = MainActivity::class.java,
    )

    companion object {
        const val EXTRA_MEDIA_TAB_ID = "dextra_media_tab_id"
        const val ACTION_PLAY = "com.dwicao.dextra.MEDIA_PLAY"
        const val ACTION_PAUSE = "com.dwicao.dextra.MEDIA_PAUSE"
        const val ACTION_STOP = "com.dwicao.dextra.MEDIA_STOP"
        private const val CHANNEL_ID = "dextra_media"
        private const val NOTIFICATION_ID = 18_001
    }
}
