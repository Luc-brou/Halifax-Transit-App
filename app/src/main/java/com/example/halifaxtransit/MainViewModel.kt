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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class MainViewModel : ViewModel() {

    private val _gtfs = MutableStateFlow<GtfsRealtime.FeedMessage?>(null)
    val gtfs = _gtfs.asStateFlow()

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes = _routes.asStateFlow()

    private val _buses = MutableStateFlow<Map<String, AnimatedBus>>(emptyMap())
    val buses = _buses.asStateFlow()

    private val _frameTime = MutableStateFlow(System.currentTimeMillis())
    val frameTime = _frameTime.asStateFlow()

    private lateinit var dao: RoutesDao

    // UNSAFE CLIENT FOR EMULATOR
    private val unsafeClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    init {
        viewModelScope.launch {
            while (true) {
                _frameTime.value = System.currentTimeMillis()
                delay(16L)
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
                    val request = Request.Builder()
                        .url("https://gtfs.halifax.ca/realtime/Vehicle/VehiclePositions.pb")
                        .build()

                    val feed = withContext(Dispatchers.IO) {
                        unsafeClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                            GtfsRealtime.FeedMessage.parseFrom(response.body!!.byteStream())
                        }
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
                            fromLat = old?.toLat ?: pos.latitude.toDouble(),
                            fromLon = old?.toLon ?: pos.longitude.toDouble(),
                            toLat = pos.latitude.toDouble(),
                            toLon = pos.longitude.toDouble(),
                            lastUpdateTime = System.currentTimeMillis()
                        )
                    }

                    _buses.value = updated

                } catch (e: Exception) {
                    Log.e("GTFS", "GTFS error: $e")
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
