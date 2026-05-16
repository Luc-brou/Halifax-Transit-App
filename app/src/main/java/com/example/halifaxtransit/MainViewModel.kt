package com.example.halifaxtransit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.halifaxtransit.database.*
import com.example.halifaxtransit.models.*
import com.google.transit.realtime.GtfsRealtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import com.opencsv.CSVReader
import java.io.InputStreamReader

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
    private lateinit var favouriteLocationDao: FavouriteLocationDao
    private lateinit var busStopDao: BusStopDao

    // -----------------------------
    // UI Toggles
    // -----------------------------
    val showBuses = MutableStateFlow(false)
    val showBusStops = MutableStateFlow(false)

    // -----------------------------
    // Search Result + Loading State
    // -----------------------------
    data class SearchResult(
        val name: String,
        val lat: Double,
        val lon: Double
    )

    val isSearching = MutableStateFlow(false)
    private var searchJob: Job? = null

    // -----------------------------
    // Favourite Locations
    // -----------------------------
    private val _favouriteLocations = MutableStateFlow<List<FavouriteLocation>>(emptyList())
    val favouriteLocations = _favouriteLocations.asStateFlow()

    // -----------------------------
    // Bus Stops
    // -----------------------------
    private val _busStops = MutableStateFlow<List<BusStop>>(emptyList())
    val busStops = _busStops.asStateFlow()

    // -----------------------------
    // Unsafe client for emulator
    // -----------------------------
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

    // -----------------------------
    // Debounced Search
    // -----------------------------
    fun searchPlacesDebounced(query: String, callback: (List<SearchResult>) -> Unit) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            searchPlaces(query, callback)
        }
    }

    // -----------------------------
    // Halifax Transit–Only Search
    // -----------------------------
    fun searchPlaces(query: String, onResult: (List<SearchResult>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isSearching.value = true

                val baseUrl =
                    "https://nominatim.openstreetmap.org/search" +
                            "?q=${query.replace(" ", "+")}" +
                            "&format=json" +
                            "&addressdetails=1" +
                            "&countrycodes=ca" +
                            "&viewbox=-63.95,44.90,-63.20,44.45" + // HRM service area
                            "&bounded=1" +
                            "&limit=10"

                val request = Request.Builder()
                    .url(baseUrl)
                    .header("User-Agent", "HalifaxTransitApp/1.0")
                    .build()

                val response = unsafeClient.newCall(request).execute()
                val body = response.body?.string() ?: "[]"
                val json = Json.parseToJsonElement(body).jsonArray

                var results = json.mapNotNull { item ->
                    val obj = item.jsonObject
                    val name = obj["display_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    val lon = obj["lon"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    SearchResult(name, lat, lon)
                }

                // -----------------------------
                // STRICT HRM COMMUNITY FILTER
                // -----------------------------
                val allowedAreas = listOf(
                    "halifax", "dartmouth", "bedford", "sackville", "lower sackville",
                    "middle sackville", "upper sackville", "spryfield", "clayton park",
                    "timberlea", "cole harbour", "eastern passage", "beaver bank",
                    "herring cove", "tantallon", "beechville", "armdale", "fairview",
                    "north end", "south end", "west end"
                )

                results = results.filter { r ->
                    val name = r.name.lowercase()
                    allowedAreas.any { area -> name.contains(area) }
                }

                // -----------------------------
                // GEO FENCE (backup)
                // -----------------------------
                results = results.filter {
                    it.lat in 44.45..44.90 && it.lon in -63.95..-63.20
                }

                isSearching.value = false
                onResult(results)

            } catch (e: Exception) {
                Log.e("SEARCH", "Search error: $e")
                isSearching.value = false
                onResult(emptyList())
            }
        }
    }

    // -----------------------------
    // Frame timer
    // -----------------------------
    init {
        viewModelScope.launch {
            while (true) {
                _frameTime.value = System.currentTimeMillis()
                delay(16L)
            }
        }
    }

    // -----------------------------
    // Database Init
    // -----------------------------
    fun initDb(context: Context) {
        val db = AppDatabase.getDatabase(context)
        dao = db.routesDao()
        favouriteLocationDao = db.favouriteLocationDao()
        busStopDao = db.busStopDao()

        viewModelScope.launch {
            dao.getAll().collect { _routes.value = it }
        }

        viewModelScope.launch {
            favouriteLocationDao.getAll().collect { _favouriteLocations.value = it }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val existing = busStopDao.getAll().firstOrNull()
            if (existing.isNullOrEmpty()) {
                val loaded = loadBusStopsFromCsv(context)
                busStopDao.insertAll(loaded)
            }

            busStopDao.getAll().collect {
                _busStops.value = it
            }
        }
    }

    // -----------------------------
    // CSV Loader
    // -----------------------------
    private fun loadBusStopsFromCsv(context: Context): List<BusStop> {
        val input = context.assets.open("BusStops.csv")
        val reader = CSVReader(InputStreamReader(input))

        val rows = reader.readAll()

        return rows.drop(1).mapNotNull { parts ->

            if (parts.size < 27) return@mapNotNull null

            val stopId = parts[1].trim()
            val stopName = parts[4].trim()

            val xStr = parts[25].trim()
            val yStr = parts[26].trim()

            val x = xStr.toDoubleOrNull() ?: return@mapNotNull null
            val y = yStr.toDoubleOrNull() ?: return@mapNotNull null

            val lon = x / 20037508.34 * 180
            var lat = y / 20037508.34 * 180
            lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI / 2)

            BusStop(
                stop_id = stopId,
                stop_name = stopName,
                stop_lat = lat,
                stop_lon = lon
            )
        }
    }

    // -----------------------------
    // GTFS Updates
    // -----------------------------
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

    // -----------------------------
    // Route Toggles
    // -----------------------------
    fun toggleHighlight(routeId: String, highlight: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setHighlight(routeId, highlight)
        }
    }

    fun toggleFavourite(routeId: String, fav: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setFavourite(routeId, fav)
        }
    }

    // -----------------------------
    // Favourite Location Functions
    // -----------------------------
    fun addFavouriteLocation(name: String, lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            favouriteLocationDao.insert(
                FavouriteLocation(
                    name = name,
                    lat = lat,
                    lon = lon
                )
            )
        }
    }

    fun removeFavouriteLocation(location: FavouriteLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            favouriteLocationDao.delete(location)
        }
    }
}
