package com.example.workmanager03.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_table")
data class LocationModel(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "date")
    var datetime: String,

    @ColumnInfo(name = "latitude")
    var latitude: String,

    @ColumnInfo(name = "longitude")
    var longitude: String,

    @ColumnInfo(name = "altitude")
    var altitude: String,

    @ColumnInfo(name = "signal")
    var signal: Int
)