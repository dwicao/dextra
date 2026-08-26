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
)

@Dao
interface BrowserDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Database(entities = [HistoryEntry::class, Bookmark::class], version = 1, exportSchema = false)
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
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
