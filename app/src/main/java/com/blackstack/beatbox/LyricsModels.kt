package com.blackstack.beatbox

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context

/**
 * Represents a single line of synchronized lyrics with timestamp
 * 
 * @property timestampMs The timestamp in milliseconds when this line should be displayed
 * @property text The lyrics text for this line
 */
data class SyncedLyricsLine(
    val timestampMs: Long,
    val text: String
)

/**
 * Represents complete synchronized lyrics for a song
 * 
 * @property lines List of synced lyrics lines sorted by timestamp
 * @property metadata Optional metadata extracted from LRC file
 */
data class SyncedLyrics(
    val lines: List<SyncedLyricsLine>,
    val metadata: LyricsMetadata = LyricsMetadata()
)

/**
 * Metadata extracted from LRC file tags
 * 
 * @property title Song title from [ti:] tag
 * @property artist Artist name from [ar:] tag
 * @property album Album name from [al:] tag
 * @property by Creator/author from [by:] tag
 * @property offset Timing offset in milliseconds from [offset:] tag
 */
data class LyricsMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val by: String? = null,
    val offset: Long = 0L
)

/**
 * Room database entity for caching lyrics
 * 
 * @property songId Unique identifier for the song (primary key)
 * @property lrcContent The lyrics content in LRC format
 * @property source Source of the lyrics ("local" or "api")
 * @property fetchedAt Timestamp when lyrics were fetched/cached
 */
@Entity(tableName = "lyrics_cache")
data class CachedLyrics(
    @PrimaryKey val songId: Long,
    val lrcContent: String,
    val source: String,
    val fetchedAt: Long
)

/**
 * Data Access Object for lyrics cache operations
 */
@Dao
interface LyricsCacheDao {
    /**
     * Retrieves cached lyrics for a specific song
     * 
     * @param songId The unique identifier of the song
     * @return CachedLyrics if found, null otherwise
     */
    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId")
    suspend fun getLyrics(songId: Long): CachedLyrics?
    
    /**
     * Inserts or replaces lyrics in the cache
     * 
     * @param lyrics The lyrics to cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: CachedLyrics)
    
    /**
     * Deletes cached lyrics for a specific song
     * 
     * @param songId The unique identifier of the song
     */
    @Query("DELETE FROM lyrics_cache WHERE songId = :songId")
    suspend fun deleteLyrics(songId: Long)
    
    /**
     * Deletes cached lyrics older than the specified timestamp
     * Useful for cache cleanup operations
     * 
     * @param timestamp The cutoff timestamp (lyrics fetched before this will be deleted)
     */
    @Query("DELETE FROM lyrics_cache WHERE fetchedAt < :timestamp")
    suspend fun deleteOldLyrics(timestamp: Long)
}

/**
 * Room database for lyrics caching
 */
@Database(entities = [CachedLyrics::class], version = 1, exportSchema = false)
abstract class LyricsDatabase : RoomDatabase() {
    /**
     * Provides access to the lyrics cache DAO
     */
    abstract fun lyricsCacheDao(): LyricsCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: LyricsDatabase? = null
        
        /**
         * Gets the singleton database instance
         * Thread-safe implementation using double-checked locking
         * 
         * @param context Application context
         * @return The LyricsDatabase instance
         */
        fun getDatabase(context: Context): LyricsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LyricsDatabase::class.java,
                    "lyrics_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
