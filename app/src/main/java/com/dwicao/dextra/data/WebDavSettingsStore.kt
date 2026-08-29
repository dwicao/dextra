package com.dwicao.dextra.data

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class WebDavConfig(
    val endpoint: String,
    val username: String,
    val password: String,
    val remoteFile: String,
    val passphrase: String,
    val intervalHours: Int = 24,
    val enabled: Boolean = true,
    val lastEtag: String? = null,
    val lastSyncAt: Long? = null,
)

data class WebDavSettingsState(
    val configured: Boolean = false,
    val endpoint: String = "",
    val username: String = "",
    val remoteFile: String = "dextra-sync.json",
    val intervalHours: Int = 24,
    val enabled: Boolean = false,
    val lastSyncAt: Long? = null,
)

/** Keeps the WebDAV password and sync passphrase encrypted at rest with Android Keystore. */
class WebDavSettingsStore(private val context: Context) {
    private val file = File(context.filesDir, "webdav.settings")

    @Synchronized
    fun load(): WebDavConfig? = runCatching {
        if (!file.isFile || file.length() == 0L) return null
        val envelope = JSONObject(file.readText(Charsets.UTF_8))
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(envelope.getString("data"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }
        val value = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        WebDavConfig(
            endpoint = value.getString("endpoint"),
            username = value.optString("username"),
            password = value.getString("password"),
            remoteFile = value.optString("remoteFile", "dextra-sync.json"),
            passphrase = value.getString("passphrase"),
            intervalHours = value.optInt("intervalHours", 24).coerceIn(1, 168),
            enabled = value.optBoolean("enabled", true),
            lastEtag = value.optString("lastEtag").takeIf(String::isNotBlank),
            lastSyncAt = value.optLong("lastSyncAt", 0L).takeIf { it > 0L },
        )
    }.getOrNull()

    @Synchronized
    fun save(config: WebDavConfig) {
        val value = JSONObject()
            .put("endpoint", config.endpoint)
            .put("username", config.username)
            .put("password", config.password)
            .put("remoteFile", config.remoteFile)
            .put("passphrase", config.passphrase)
            .put("intervalHours", config.intervalHours.coerceIn(1, 168))
            .put("enabled", config.enabled)
            .put("lastEtag", config.lastEtag)
            .put("lastSyncAt", config.lastSyncAt)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key())
        }
        val envelope = JSONObject()
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(envelope.toString(), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }
    }

    @Synchronized
    fun clear() {
        runCatching { file.delete() }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", ANDROID_KEYSTORE).apply {
            init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "dextra.webdav"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
    }
}
