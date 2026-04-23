package com.example.halifaxtransit.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.halifaxtransit.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(viewModel: MainViewModel) {

    val routes by viewModel.routes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf("Number") }

    val modes = listOf("Number", "Location")

    // 🔍 FILTER (startsWith only)
    val filteredRoutes = remember(routes, searchQuery, searchMode) {
        val query = searchQuery.trim().lowercase()

        val result = if (query.isBlank()) {
            routes
        } else {
            routes.filter { route ->
                when (searchMode) {
                    "Number" ->
                        route.routeShortName.lowercase().startsWith(query)

                    "Location" ->
                        route.routeLongName.lowercase().startsWith(query)

                    else -> true
                }
            }
        }

        result.sortedBy { it.routeShortName }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔥 SINGLE SEARCH BAR
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),

                singleLine = true,

                placeholder = {
                    Text(
                        if (searchMode == "Number") "Search route (e.g. 8C)"
                        else "Search location (e.g. Halifax)"
                    )
                },

                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },

                trailingIcon = {
                    Row {
                        Text(
                            text = searchMode,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
            )

            // 🔽 DROPDOWN MENU
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                modes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode) },
                        onClick = {
                            searchMode = mode
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            items(
                items = filteredRoutes,
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
}