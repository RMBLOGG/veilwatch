package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistory>>

    @Query("SELECT * FROM watch_history WHERE anime_id = :animeId ORDER BY timestamp DESC LIMIT 1")
    fun getHistoryForAnime(animeId: Int): Flow<WatchHistory?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistory)

    @Query("DELETE FROM watch_history WHERE anime_id = :animeId")
    suspend fun deleteHistoryForAnime(animeId: Int)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY updated_at DESC")
    fun getWatchlist(): Flow<List<WatchlistItem>>

    @Query("SELECT * FROM watchlist WHERE id = :id LIMIT 1")
    fun getWatchlistItem(id: Int): Flow<WatchlistItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteWatchlistItem(id: Int)

    @Query("DELETE FROM watchlist")
    suspend fun clearWatchlist()
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    fun getPreference(key: String): Flow<PreferenceItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceItem)

    @Query("DELETE FROM preferences WHERE `key` = :key")
    suspend fun deletePreference(key: String)
}

@Database(
    entities = [WatchHistory::class, WatchlistItem::class, PreferenceItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun preferencesDao(): PreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "veilwatch_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
