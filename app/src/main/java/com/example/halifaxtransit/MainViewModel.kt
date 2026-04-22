package com.example.halifaxtransit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.halifaxtransit.database.AppDatabase
import com.example.halifaxtransit.database.RoutesDao
import com.example.halifaxtransit.models.AnimatedBus
import com.example.halifaxtransit.models.Route
import com.google.transit.realtime.GtfsRealtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainViewModel : ViewModel() {

    private val _gtfs = MutableStateFlow<GtfsRealtime.FeedMessage?>(null)
    val gtfs = _gtfs.asStateFlow()

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes = _routes.asStateFlow()

    private val _buses = MutableStateFlow<Map<String, AnimatedBus>>(emptyMap())
    val buses = _buses.asStateFlow()

    // 🔥 GLOBAL ANIMATION CLOCK (THIS FIXES JITTER)
    private val _frameTime = MutableStateFlow(System.currentTimeMillis())
    val frameTime = _frameTime.asStateFlow()

    private lateinit var dao: RoutesDao

    init {
        viewModelScope.launch {
            while (true) {
                _frameTime.value = System.currentTimeMillis()
                delay(16L) // ~60fps render clock
            }
        }
    }

    fun initDb(context: Context) {
        dao = AppDatabase.getDatabase(context).routesDao()

        viewModelScope.launch {
            dao.getAll().collect {
                _routes.value = it
            }
        }
    }

    fun startGtfsUpdates() {
        viewModelScope.launch {
            while (true) {
                try {
                    val url = URL(
                        "https://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb"
                    )

                    val feed = withContext(Dispatchers.IO) {
                        GtfsRealtime.FeedMessage.parseFrom(url.openStream())
                    }

                    _gtfs.value = feed

                    val updated = _buses.value.toMutableMap()

                    feed.entityList.forEach { entity ->
                        val v = entity.vehicle ?: return@forEach
                        val pos = v.position ?: return@forEach

                        val id = entity.id
                        val routeId = v.trip?.routeId ?: "?"

                        val old = updated[id]

                        updated[id] = AnimatedBus(
                            id = id,
                            routeId = routeId,

                            // 🔥 IMPORTANT: NEVER reset from wrong origin
                            fromLat = old?.toLat ?: pos.latitude.toDouble(),
                            fromLon = old?.toLon ?: pos.longitude.toDouble(),

                            toLat = pos.latitude.toDouble(),
                            toLon = pos.longitude.toDouble(),

                            lastUpdateTime = System.currentTimeMillis()
                        )
                    }

                    _buses.value = updated

                } catch (e: Exception) {
                    Log.e("GTFS", e.toString())
                }

                delay(5000L)
            }
        }
    }

    fun toggleHighlight(routeId: String, highlight: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setHighlight(routeId, highlight)
        }
    }
}