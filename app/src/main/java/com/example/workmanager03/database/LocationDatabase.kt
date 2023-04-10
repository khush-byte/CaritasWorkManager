package com.example.workmanager03.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocationModel::class], version = 1)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}