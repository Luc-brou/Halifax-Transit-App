package com.example.halifaxtransit.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.halifaxtransit.MainViewModel
import com.example.halifaxtransit.R

// MUST be outside composable
enum class RouteFilter {
    ALL,
    FAVOURITE,
    HIGHLIGHTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(viewModel: MainViewModel) {

    val routes by viewModel.routes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf("Number") }

    val modes = listOf("Number", "Location")

    var filterMode by remember { mutableStateOf(RouteFilter.ALL) }

    // -----------------------------
    // FILTERING LOGIC
    // -----------------------------
    val filteredRoutes = remember(routes, searchQuery, searchMode, filterMode) {

        val query = searchQuery.trim().lowercase()

        val base = if (query.isBlank()) {
            routes
        } else {
            routes.filter { route ->

                when (searchMode) {

                    "Number" -> {
                        val short = route.routeShortName.lowercase()

                        when {
                            // exact match
                            short == query -> true

                            // 2+ digits → allow prefix match
                            query.length >= 2 && short.startsWith(query) -> true

                            // 1 digit → ONLY match single-digit routes
                            query.length == 1 && short.length == 1 && short == query -> true

                            else -> false
                        }
                    }

                    "Location" ->
                        route.routeLongName.lowercase().contains(query)

                    else -> true
                }
            }
        }

        // Apply filter mode
        val filtered = when (filterMode) {
            RouteFilter.ALL -> base
            RouteFilter.FAVOURITE -> base.filter { it.favourite }
            RouteFilter.HIGHLIGHTED -> base.filter { it.highlights }
        }

        filtered.sortedBy { it.routeShortName }
    }

    // -----------------------------
    // UI LAYOUT
    // -----------------------------
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // SEARCH BAR + DROPDOWN
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery =
                            if (searchMode == "Number") {
                                newValue.filter { it.isDigit() } // ONLY digits allowed
                            } else {
                                newValue
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            if (searchMode == "Number") "Search route (e.g. 8)"
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

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    modes.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = {
                                searchMode = mode
                                searchQuery = "" // reset input when switching modes
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROUTE LIST
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column(Modifier.weight(1f)) {
                            Text(route.routeShortName)
                            Text(route.routeLongName)
                        }

                        // ⭐ FAVOURITE STAR
                        IconButton(
                            onClick = {
                                viewModel.toggleFavourite(route.routeId, !route.favourite)
                            }
                        ) {
                            val icon = if (route.favourite)
                                R.drawable.stariconfilled
                            else
                                R.drawable.staricon

                            Image(
                                painter = painterResource(icon),
                                contentDescription = "Favourite",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // ✔ HIGHLIGHT CHECKBOX
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

        // ⭐ FLOATING FILTER BUTTON (cycles through 3 modes)
        FloatingActionButton(
            onClick = {
                filterMode = when (filterMode) {
                    RouteFilter.ALL -> RouteFilter.FAVOURITE
                    RouteFilter.FAVOURITE -> RouteFilter.HIGHLIGHTED
                    RouteFilter.HIGHLIGHTED -> RouteFilter.ALL
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            val icon = when (filterMode) {
                RouteFilter.ALL -> R.drawable.listicon
                RouteFilter.FAVOURITE -> R.drawable.stariconfilled
                RouteFilter.HIGHLIGHTED -> R.drawable.busblue
            }

            Image(
                painter = painterResource(icon),
                contentDescription = "Filter mode",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
