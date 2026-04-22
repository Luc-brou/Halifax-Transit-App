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

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes = _routes.asStateFlow()

    private val _buses = MutableStateFlow<Map<String, AnimatedBus>>(emptyMap())
    val buses = _buses.asStateFlow()

    private lateinit var dao: RoutesDao

    // ✅ RESTORED: called from Activity
    fun initDb(context: Context) {
        dao = AppDatabase.getDatabase(context).routesDao()

        viewModelScope.launch {
            dao.getAll().collect {
                _routes.value = it
            }
        }
    }

    // ✅ GTFS LIVE UPDATES
    fun startGtfsUpdates() {
        viewModelScope.launch {

            while (true) {
                try {
                    val feed = withContext(Dispatchers.IO) {
                        val url = URL(
                            "https://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb"
                        )
                        GtfsRealtime.FeedMessage.parseFrom(url.openStream())
                    }

                    val now = System.currentTimeMillis()
                    val updated = _buses.value.toMutableMap()

                    feed.entityList.forEach { entity ->
                        val v = entity.vehicle ?: return@forEach
                        val pos = v.position ?: return@forEach

                        val id = entity.id
                        val routeId = v.trip?.routeId ?: "?"

                        val newLat = pos.latitude.toDouble()
                        val newLon = pos.longitude.toDouble()

                        val old = updated[id]

                        updated[id] = AnimatedBus(
                            id = id,
                            routeId = routeId,

                            prevLat = old?.currLat ?: newLat,
                            prevLon = old?.currLon ?: newLon,

                            currLat = newLat,
                            currLon = newLon,

                            prevTime = old?.currTime ?: now,
                            currTime = now
                        )
                    }

                    _buses.value = updated

                } catch (e: Exception) {
                    Log.e("GTFS", e.toString())
                }

                delay(5000)
            }
        }
    }

    fun toggleHighlight(routeId: String, highlight: Boolean) {
        if (!::dao.isInitialized) return

        viewModelScope.launch(Dispatchers.IO) {
            dao.setHighlight(routeId, highlight)
        }
    }
}