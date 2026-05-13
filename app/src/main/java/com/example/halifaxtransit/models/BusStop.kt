package com.example.halifaxtransit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "BusStops")
data class BusStop(
    @PrimaryKey val stop_id: String,
    val stop_name: String,
    val stop_lat: Double,
    val stop_lon: Double
)
