package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "geocode_cache")
data class GeocodeCacheEntity(
    @PrimaryKey val addressQuery: String,
    val lat: Double,
    val lng: Double,
    val displayName: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_stops")
data class SavedStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val addressText: String,
    val lat: Double,
    val lng: Double,
    val isOrigin: Boolean,
    val orderIndex: Int = 0
)

@Dao
interface GeocodeCacheDao {
    @Query("SELECT * FROM geocode_cache WHERE addressQuery = :query LIMIT 1")
    suspend fun getGeocode(query: String): GeocodeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeocode(entity: GeocodeCacheEntity)

    @Query("DELETE FROM geocode_cache")
    suspend fun clearCache()
}

@Dao
interface SavedStopDao {
    @Query("SELECT * FROM saved_stops ORDER BY isOrigin DESC, orderIndex ASC")
    fun getAllStopsFlow(): Flow<List<SavedStopEntity>>

    @Query("SELECT * FROM saved_stops ORDER BY isOrigin DESC, orderIndex ASC")
    suspend fun getAllStops(): List<SavedStopEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: SavedStopEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<SavedStopEntity>)

    @Query("DELETE FROM saved_stops WHERE id = :id")
    suspend fun deleteStop(id: Long)

    @Query("DELETE FROM saved_stops")
    suspend fun clearAllStops()

    @Query("DELETE FROM saved_stops WHERE isOrigin = 0")
    suspend fun clearDestinations()
}

@Database(entities = [GeocodeCacheEntity::class, SavedStopEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun geocodeCacheDao(): GeocodeCacheDao
    abstract fun savedStopDao(): SavedStopDao
}
