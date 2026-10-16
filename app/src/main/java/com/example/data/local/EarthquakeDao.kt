package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EarthquakeDao {
    @Query("SELECT * FROM earthquakes ORDER BY timeEpochMillis DESC")
    fun getAllEarthquakesFlow(): Flow<List<EarthquakeEntity>>

    @Query("SELECT * FROM earthquakes ORDER BY timeEpochMillis DESC LIMIT 100")
    suspend fun getRecentEarthquakes(): List<EarthquakeEntity>

    @Query("SELECT * FROM earthquakes WHERE isAutoGempa = 1 LIMIT 1")
    suspend fun getLatestAutoGempa(): EarthquakeEntity?

    @Query("SELECT * FROM earthquakes WHERE id = :id LIMIT 1")
    suspend fun getEarthquakeById(id: String): EarthquakeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEarthquakes(earthquakes: List<EarthquakeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEarthquake(earthquake: EarthquakeEntity)

    @Query("DELETE FROM earthquakes WHERE id NOT IN (SELECT earthquakeId FROM favorites) AND isAutoGempa = 0 AND timeEpochMillis < :olderThanMillis")
    suspend fun deleteOldEarthquakes(olderThanMillis: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT earthquakeId FROM favorites")
    fun getAllFavoriteIdsFlow(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE earthquakeId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE earthquakeId = :id")
    suspend fun removeFavorite(id: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM notification_logs WHERE earthquakeId = :id)")
    suspend fun hasBeenNotified(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logNotification(log: NotificationLogEntity)

    @Query("SELECT COUNT(*) FROM notification_logs")
    suspend fun getLogCount(): Int
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items")
    fun getAllChecklistItemsFlow(): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItems(): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Query("UPDATE checklist_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateCheckedStatus(id: String, isChecked: Boolean)
}
