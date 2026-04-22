package com.example.halifaxtransit.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.halifaxtransit.MainViewModel
import com.google.transit.realtime.GtfsRealtime
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

@Composable
fun BusMapScreen(viewModel: MainViewModel) {

    val gtfs by viewModel.gtfs.collectAsState()
    val routes by viewModel.routes.collectAsState()

    val buses = gtfs?.entityList

    val mapState = rememberMapViewportState {
        setCameraOptions {
            zoom(12.0)
            center(Point.fromLngLat(-63.58, 44.65))
        }
    }

    MapboxMap(mapViewportState = mapState) {

        if (!buses.isNullOrEmpty()) {
            buses.forEach { entity ->

                val vehicle = entity.vehicle ?: return@forEach
                val pos = vehicle.position ?: return@forEach

                val routeId = vehicle.trip?.routeId ?: "?"

                val route = routes.find {
                    routeId.startsWith(it.routeId)
                }

                val highlight = route?.highlights == true

                val icon = if (highlight)
                    com.example.halifaxtransit.R.drawable.busblue
                else
                    com.example.halifaxtransit.R.drawable.bus

                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(
                            Point.fromLngLat(
                                pos.longitude.toDouble(),
                                pos.latitude.toDouble()
                            )
                        )
                    }
                ) {
                    Column {

                        Image(
                            painter = painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            contentScale = ContentScale.Crop
                        )

                        Text(
                            routeId,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}