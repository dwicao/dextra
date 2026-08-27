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
)

enum class DownloadStatus(val label: String) {
    QUEUED("Queued"),
    DOWNLOADING("Downloading"),
    PAUSED("Paused"),
    COMPLETE("Complete"),
    FAILED("Failed"),
    CANCELED("Canceled"),
}

@Dao
interface BrowserDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    fun observeHistory(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<Bookmark>>

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

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permission = :permission LIMIT 1")
    suspend fun getSitePermission(origin: String, permission: String): SitePermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSitePermission(permission: SitePermission)

    @Query("DELETE FROM site_permissions")
    suspend fun clearSitePermissions()

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
}

@Database(entities = [HistoryEntry::class, Bookmark::class, DownloadEntry::class, SitePermission::class], version = 7, exportSchema = false)
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build().also { instance = it }
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
    }
}
