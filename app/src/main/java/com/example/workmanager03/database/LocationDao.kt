package com.example.workmanager03.database

import androidx.room.*

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocation(locationModel: LocationModel)

    @Query("SELECT * FROM location_table")
    fun getAllLocations(): List<LocationModel>

    @Query("SELECT * FROM location_table LIMIT 1")
    fun getLocation(): LocationModel

    @Update
    suspend fun updateLocation(locationModel: LocationModel)

    @Delete
    suspend fun deleteDelete(locationModel: LocationModel)
}