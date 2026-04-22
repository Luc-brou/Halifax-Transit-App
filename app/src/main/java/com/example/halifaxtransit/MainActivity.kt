package com.example.halifaxtransit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.halifaxtransit.screens.BusMapScreen
import com.example.halifaxtransit.screens.RoutesScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.startGtfsUpdates()

        setContent {

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            Scaffold(
                bottomBar = {
                    NavigationBar {

                        NavigationBarItem(
                            selected = currentRoute == "map",
                            onClick = {
                                navController.navigate("map") {
                                    popUpTo("map") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            label = { Text("Map") },
                            icon = {}
                        )

                        NavigationBarItem(
                            selected = currentRoute == "routes",
                            onClick = {
                                navController.navigate("routes") {
                                    popUpTo("map")
                                    launchSingleTop = true
                                }
                            },
                            label = { Text("Routes") },
                            icon = {}
                        )
                    }
                }
            ) { padding ->

                NavHost(
                    navController = navController,
                    startDestination = "map",
                    modifier = Modifier.padding(padding)
                ) {

                    composable("map") {
                        BusMapScreen(viewModel)
                    }

                    composable("routes") {
                        RoutesScreen(viewModel)
                    }
                }
            }
        }
    }
}