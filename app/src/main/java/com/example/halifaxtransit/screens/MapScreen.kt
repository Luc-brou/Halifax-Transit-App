package com.example.halifaxtransit.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.halifaxtransit.MainViewModel
import com.example.halifaxtransit.R
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.*
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.delay

@Composable
fun BusMapScreen(viewModel: MainViewModel) {

    val buses by viewModel.buses.collectAsState()
    val routes by viewModel.routes.collectAsState()

    val mapState = rememberMapViewportState {
        setCameraOptions {
            zoom(11.7)
            center(Point.fromLngLat(-63.585, 44.648))
        }
    }

    MapboxMap(mapViewportState = mapState) {

        val now = remember { mutableStateOf(System.currentTimeMillis()) }

        LaunchedEffect(Unit) {
            while (true) {
                now.value = System.currentTimeMillis()
                delay(33) // ~30fps smooth animation
            }
        }

        buses.values.forEach { bus ->

            val duration = (bus.currTime - bus.prevTime).coerceAtLeast(1L)
            val t = ((now.value - bus.currTime).toFloat() / duration).coerceIn(0f, 1f)

            val lat = bus.prevLat + (bus.currLat - bus.prevLat) * t
            val lon = bus.prevLon + (bus.currLon - bus.prevLon) * t

            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@forEach

            val route = routes.find {
                bus.routeId.trim().startsWith(it.routeId.trim())
            }

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
                    modifier = Modifier.padding(0.dp)
                ) {

                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp) // FIXED SCALE
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