package com.example.halifaxtransit.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.halifaxtransit.MainViewModel

@Composable
fun FavouritesScreen(viewModel: MainViewModel) {

    val favourites by viewModel.favouriteLocations.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Favourite Locations", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        favourites.forEach { fav ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(Modifier.weight(1f)) {
                    Text(fav.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${fav.lat}, ${fav.lon}", style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    text = "🗑️",
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            viewModel.removeFavouriteLocation(fav)
                        }
                )
            }
        }
    }
}

