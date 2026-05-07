package com.example.halifaxtransit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FavouriteLocations")
data class FavouriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lat: Double,
    val lon: Double
)