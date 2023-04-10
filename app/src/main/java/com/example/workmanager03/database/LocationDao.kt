package com.example.workmanager03.database

import androidx.room.*

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(locationModel: LocationModel)

    @Query("SELECT * FROM location_table")
    suspend fun getAllLocations(): List<LocationModel>

    @Query("SELECT * FROM location_table LIMIT 1")
    suspend fun getLocation(): LocationModel

    @Query("SELECT * FROM location_table ORDER BY ID DESC LIMIT 1")
    suspend fun getPreviousLocation(): LocationModel

    @Update
    suspend fun updateLocation(locationModel: LocationModel)

    @Delete
    suspend fun deleteLocation(locationModel: LocationModel)
}