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

    val mapState = rememberMapViewportState {
        setCameraOptions {
            zoom(11.5)
            center(Point.fromLngLat(-63.585, 44.648))
        }
    }

    MapboxMap(mapViewportState = mapState) {

        buses.values.forEach { bus ->

            val duration = (frameTime - bus.lastUpdateTime).coerceAtLeast(1L)

            val t = (duration / 2000f).coerceIn(0f, 1f)

            val lat = bus.fromLat + (bus.toLat - bus.fromLat) * t
            val lon = bus.fromLon + (bus.toLon - bus.fromLon) * t

            val route = routes.find {
                bus.routeId.startsWith(it.routeId)
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