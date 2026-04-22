package com.example.halifaxtransit.models

data class AnimatedBus(
    val id: String,
    val routeId: String,

    val prevLat: Double,
    val prevLon: Double,

    val currLat: Double,
    val currLon: Double,

    val prevTime: Long,
    val currTime: Long
)