package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    @Query("SELECT EXISTS(SELECT * FROM bookmarks WHERE url = :url)")
    fun isBookmarked(url: String): Flow<Boolean>
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getHistory(): Flow<List<History>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: History)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM extensions")
    fun getAllExtensions(): Flow<List<Extension>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: Extension)

    @Query("UPDATE extensions SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateExtensionStatus(id: String, isEnabled: Boolean)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun removeExtension(id: String)
}

@Database(entities = [Bookmark::class, History::class, Extension::class], version = 3, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun extensionDao(): ExtensionDao
}
