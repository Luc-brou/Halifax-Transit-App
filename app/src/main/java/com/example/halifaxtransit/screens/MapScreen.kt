package com.example.halifaxtransit.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.halifaxtransit.MainViewModel
import com.example.halifaxtransit.R
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

@Composable
fun BusMapScreen(viewModel: MainViewModel) {

    val buses by viewModel.buses.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val frameTime by viewModel.frameTime.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // Search state
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MainViewModel.SearchResult>>(emptyList()) }

    val mapState = rememberMapViewportState {
        setCameraOptions {
            zoom(11.5)
            center(Point.fromLngLat(-63.585, 44.648))
        }
    }

    Column {

        // -----------------------------
        // SEARCH BAR
        // -----------------------------
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (query.length >= 2) {
                    viewModel.searchPlacesDebounced(query) { list ->
                        results = list
                    }
                } else {
                    results = emptyList()
                }
            },
            label = { Text("Search places") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // -----------------------------
        // LOADING INDICATOR
        // -----------------------------
        if (isSearching && query.length >= 2) {
            Text(
                "Searching…",
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                color = Color.Gray
            )
        }

        // -----------------------------
        // NO RESULTS MESSAGE
        // -----------------------------
        if (!isSearching && results.isEmpty() && query.length >= 2) {
            Text(
                "No results found",
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                color = Color.Gray
            )
        }

        // -----------------------------
        // SEARCH RESULTS LIST
        // -----------------------------
        results.forEach { result ->
            Text(
                text = result.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable {
                        mapState.setCameraOptions {
                            center(Point.fromLngLat(result.lon, result.lat))
                            zoom(14.0)
                        }
                        results = emptyList()
                    }
            )
        }

        // -----------------------------
        // MAP + BUS MARKERS
        // -----------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { results = emptyList() }
        ) {
            MapboxMap(mapViewportState = mapState) {

                buses.values.forEach { bus ->

                    val duration = (frameTime - bus.lastUpdateTime).coerceAtLeast(1L)
                    val t = (duration / 2000f).coerceIn(0f, 1f)

                    val lat = bus.fromLat + (bus.toLat - bus.fromLat) * t
                    val lon = bus.fromLon + (bus.toLon - bus.fromLon) * t

                    // -----------------------------
                    // FIXED ROUTE MATCHING
                    // -----------------------------
                    // Normalize GTFS route IDs like "1-1", "1A", etc.
                    val normalizedBusRoute = bus.routeId
                        .replace("-", "")
                        .replace("A", "")
                        .replace("B", "")
                        .trim()

                    val route = routes.find { normalizedBusRoute == it.routeId }

                    val icon = if (route?.highlights == true)
                        R.drawable.busblue
                    else
                        R.drawable.bus

                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(lon, lat))
                        }
                    ) {
                        Column(
                            modifier = Modifier.width(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Image(
                                painter = painterResource(icon),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )

                            Text(
                                bus.routeId,
                                fontSize = 10.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
