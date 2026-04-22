package com.example.halifaxtransit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.*
import com.example.halifaxtransit.screens.BusMapScreen
import com.example.halifaxtransit.screens.RoutesScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.startGtfsUpdates()

        setContent {
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            val navController = rememberNavController()
            val backStack by navController.currentBackStackEntryAsState()
            val route = backStack?.destination?.route

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = route == "map",
                            onClick = { navController.navigate("map") },
                            icon = { Icon(Icons.Default.Place, null) },
                            label = { Text("Map") }
                        )
                        NavigationBarItem(
                            selected = route == "routes",
                            onClick = { navController.navigate("routes") },
                            icon = { Icon(Icons.Default.Info, null) },
                            label = { Text("Routes") }
                        )
                    }
                }
            ) { padding ->

                NavHost(
                    navController,
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