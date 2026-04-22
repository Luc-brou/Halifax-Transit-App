package com.example.halifaxtransit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.halifaxtransit.database.AppDatabase
import com.example.halifaxtransit.database.RoutesDao
import com.example.halifaxtransit.models.Route
import com.google.transit.realtime.GtfsRealtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.URL

class MainViewModel : ViewModel() {

    private val _gtfs = MutableStateFlow<GtfsRealtime.FeedMessage?>(null)
    val gtfs = _gtfs.asStateFlow()

    private lateinit var dao: RoutesDao

    val routes = MutableStateFlow<List<Route>>(emptyList())

    fun initDb(context: Context) {
        dao = AppDatabase.getDatabase(context).routesDao()

        viewModelScope.launch {
            dao.getAll().collect {
                routes.value = it
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

                } catch (e: Exception) {
                    Log.e("GTFS", e.toString())
                }

                delay(15000)
            }
        }
    }

    fun toggleHighlight(routeId: String, highlight: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setHighlight(routeId, highlight)
        }
    }
}