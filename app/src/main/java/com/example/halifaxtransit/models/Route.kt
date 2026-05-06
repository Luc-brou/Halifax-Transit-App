package com.example.halifaxtransit.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Routes")
data class Route(
    @PrimaryKey
    @ColumnInfo(name = "route_id")
    val routeId: String,

    @ColumnInfo(name = "route_short_name")
    val routeShortName: String,

    @ColumnInfo(name = "route_long_name")
    val routeLongName: String,

    // existing highlight field (map highlighting)
    @ColumnInfo(name = "Highlights")
    val highlights: Boolean,

    @ColumnInfo(name = "favourite")
    val favourite: Boolean = false
)
