package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "anime_id") val animeId: Int,
    @ColumnInfo(name = "anime_title") val animeTitle: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "progress_seconds") val progressSeconds: Long,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: Int, // AniList/MAL Media ID
    @ColumnInfo(name = "anime_id") val animeId: Int,
    @ColumnInfo(name = "anime_title") val animeTitle: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    @ColumnInfo(name = "status") val status: String, // e.g. "CURRENT" (Watching), "COMPLETED", "PLANNING", "ON_HOLD", "DROPPED"
    @ColumnInfo(name = "score") val score: Double,
    @ColumnInfo(name = "progress") val progress: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "preferences")
data class PreferenceItem(
    @PrimaryKey val key: String,
    val value: String
)
