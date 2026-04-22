package com.example.halifaxtransit.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.halifaxtransit.MainViewModel

@Composable
fun RoutesScreen(viewModel: MainViewModel) {

    val routes by viewModel.routes.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        items(
            items = routes,
            key = { it.routeId }
        ) { route ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, Color.Gray)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(Modifier.weight(1f)) {
                    Text(route.routeShortName)
                    Text(route.routeLongName)
                }

                Checkbox(
                    checked = route.highlights,
                    onCheckedChange = { checked ->
                        viewModel.toggleHighlight(route.routeId, checked)
                    }
                )
            }
        }
    }
}