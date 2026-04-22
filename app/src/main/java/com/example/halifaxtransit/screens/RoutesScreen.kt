package com.example.halifaxtransit.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.halifaxtransit.MainViewModel

@Composable
fun RoutesScreen(viewModel: MainViewModel) {

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initDb(context)
    }

    val routes by viewModel.routes.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(routes) { route ->

            Row(
                Modifier
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
                    onCheckedChange = {
                        viewModel.toggleHighlight(route.routeId, it)
                    }
                )
            }
        }
    }
}