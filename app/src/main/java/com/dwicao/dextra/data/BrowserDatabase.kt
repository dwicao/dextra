package com.dwicao.dextra.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "history",
    indices = [Index(value = ["url"]), Index(value = ["visitedAt"])],
)
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val visitedAt: Long,
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true)],
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val createdAt: Long,
    val folder: String? = null,
)

@Entity(tableName = "site_permissions", primaryKeys = ["origin", "permission"])
data class SitePermission(
    val origin: String,
    val permission: String,
    val decision: String,
    val updatedAt: Long,
)

@Entity(tableName = "site_settings")
data class SiteSetting(
    @PrimaryKey val origin: String,
    val desktopSites: Boolean? = null,
    val adBlockingEnabled: Boolean? = null,
    val userScriptsEnabled: Boolean? = null,
    val zoomPercent: Int? = null,
    val updatedAt: Long,
)

@Entity(
    tableName = "reading_list",
    indices = [Index(value = ["url"], unique = true)],
)
data class ReadingListEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val savedAt: Long,
    val isRead: Boolean = false,
    val offlinePath: String? = null,
)

@Entity(tableName = "downloads")
data class DownloadEntry(
    @PrimaryKey val downloadId: Long,
    val fileName: String,
    val url: String,
    val mimeType: String?,
    val status: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localUri: String?,
    val filePath: String?,
    val reason: String?,
    val speedBytesPerSecond: Long,
    val createdAt: Long,
    val isPrivate: Boolean = false,
    val attempts: Int = 0,
    val destinationTreeUri: String? = null,
)

enum class DownloadStatus(val label: String) {
    QUEUED("Queued"),
    DOWNLOADING("Downloading"),
    PAUSED("Paused"),
    COMPLETE("Complete"),
    FAILED("Failed"),
    CANCELED("Canceled"),
}

@Entity(
    tableName = "installed_web_apps",
    indices = [Index(value = ["origin"], unique = true)],
)
data class InstalledWebApp(
    @PrimaryKey val id: String,
    val origin: String,
    val name: String,
    val startUrl: String,
    val scope: String,
    val installedAt: Long,
)

@Dao
interface BrowserDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    fun observeHistory(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 500")
    suspend fun searchHistory(query: String): List<HistoryEntry>

    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    suspend fun getHistory(): List<HistoryEntry>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR COALESCE(folder, '') LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchBookmarks(query: String): List<Bookmark>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY visitedAt DESC LIMIT 500)")
    suspend fun trimHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("UPDATE bookmarks SET folder = :folder WHERE url = :url")
    suspend fun updateBookmarkFolder(url: String, folder: String?)

    @Query("UPDATE bookmarks SET title = :title, folder = :folder WHERE url = :url")
    suspend fun updateBookmark(url: String, title: String, folder: String?)

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getBookmarks(): List<Bookmark>

    @Query("SELECT * FROM site_permissions ORDER BY updatedAt DESC")
    suspend fun getSitePermissions(): List<SitePermission>

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    suspend fun deleteHistoryMatching(query: String)

    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permission = :permission LIMIT 1")
    suspend fun getSitePermission(origin: String, permission: String): SitePermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSitePermission(permission: SitePermission)

    @Query("DELETE FROM site_permissions")
    suspend fun clearSitePermissions()

    @Query("SELECT * FROM site_permissions ORDER BY updatedAt DESC")
    fun observeSitePermissions(): Flow<List<SitePermission>>

    @Query("DELETE FROM site_permissions WHERE origin = :origin")
    suspend fun deleteSitePermissions(origin: String)

    @Query("SELECT * FROM site_settings WHERE origin = :origin LIMIT 1")
    suspend fun getSiteSetting(origin: String): SiteSetting?

    @Query("SELECT * FROM site_settings")
    suspend fun getSiteSettings(): List<SiteSetting>

    @Query("SELECT * FROM site_settings ORDER BY updatedAt DESC")
    fun observeSiteSettings(): Flow<List<SiteSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSiteSetting(setting: SiteSetting)

    @Query("DELETE FROM site_settings WHERE origin = :origin")
    suspend fun deleteSiteSetting(origin: String)

    @Query("DELETE FROM site_settings")
    suspend fun clearSiteSettings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingListEntry(entry: ReadingListEntry)

    @Query("SELECT * FROM reading_list ORDER BY savedAt DESC")
    fun observeReadingList(): Flow<List<ReadingListEntry>>

    @Query("SELECT * FROM reading_list WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    suspend fun searchReadingList(query: String): List<ReadingListEntry>

    @Query("SELECT * FROM reading_list WHERE url = :url LIMIT 1")
    suspend fun getReadingListEntry(url: String): ReadingListEntry?

    @Query("DELETE FROM reading_list WHERE url = :url")
    suspend fun deleteReadingListEntry(url: String)

    @Query("UPDATE reading_list SET isRead = :isRead WHERE url = :url")
    suspend fun setReadingListRead(url: String, isRead: Boolean)

    @Query("SELECT * FROM reading_list ORDER BY savedAt DESC")
    suspend fun getReadingList(): List<ReadingListEntry>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntry>>

    @Query("SELECT * FROM downloads")
    suspend fun getDownloads(): List<DownloadEntry>

    @Query("SELECT * FROM downloads WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getDownload(downloadId: Long): DownloadEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: DownloadEntry)

    @Query("DELETE FROM downloads WHERE downloadId = :downloadId")
    suspend fun deleteDownload(downloadId: Long)

    @Query("DELETE FROM downloads WHERE status IN (:statuses)")
    suspend fun deleteDownloadsWithStatus(statuses: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstalledWebApp(app: InstalledWebApp)

    @Query("SELECT * FROM installed_web_apps ORDER BY installedAt DESC")
    fun observeInstalledWebApps(): Flow<List<InstalledWebApp>>

    @Query("DELETE FROM installed_web_apps WHERE id = :id")
    suspend fun deleteInstalledWebApp(id: String)

    @Query("SELECT * FROM installed_web_apps WHERE id = :id LIMIT 1")
    suspend fun getInstalledWebApp(id: String): InstalledWebApp?

    @Query("SELECT * FROM installed_web_apps ORDER BY installedAt DESC")
    suspend fun getInstalledWebApps(): List<InstalledWebApp>
}

@Database(entities = [HistoryEntry::class, Bookmark::class, DownloadEntry::class, SitePermission::class, SiteSetting::class, ReadingListEntry::class, InstalledWebApp::class], version = 11, exportSchema = false)
abstract class BrowserDatabase : androidx.room.RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var instance: BrowserDatabase? = null

        fun get(context: Context): BrowserDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BrowserDatabase::class.java,
                "dextra.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloads (
                        downloadId INTEGER NOT NULL,
                        fileName TEXT NOT NULL,
                        url TEXT NOT NULL,
                        mimeType TEXT,
                        status TEXT NOT NULL,
                        bytesDownloaded INTEGER NOT NULL,
                        totalBytes INTEGER NOT NULL,
                        localUri TEXT,
                        reason TEXT,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(downloadId)
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN filePath TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN speedBytesPerSecond INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE bookmarks ADD COLUMN folder TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS site_permissions (
                        origin TEXT NOT NULL,
                        permission TEXT NOT NULL,
                        decision TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(origin, permission)
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS site_settings (
                        origin TEXT NOT NULL,
                        desktopSites INTEGER,
                        adBlockingEnabled INTEGER,
                        userScriptsEnabled INTEGER,
                        zoomPercent INTEGER,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(origin)
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_list (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        savedAt INTEGER NOT NULL,
                        isRead INTEGER NOT NULL,
                        offlinePath TEXT
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reading_list_url ON reading_list(url)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN destinationTreeUri TEXT")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS installed_web_apps (
                        id TEXT NOT NULL,
                        origin TEXT NOT NULL,
                        name TEXT NOT NULL,
                        startUrl TEXT NOT NULL,
                        scope TEXT NOT NULL,
                        installedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_installed_web_apps_origin ON installed_web_apps(origin)")
            }
        }
    }
}
