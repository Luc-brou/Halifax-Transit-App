package com.example.halifaxtransit.models

data class AnimatedBus(
    val id: String,
    val routeId: String,

    val fromLat: Double,
    val fromLon: Double,
    val toLat: Double,
    val toLon: Double,

    val lastUpdateTime: Long
)