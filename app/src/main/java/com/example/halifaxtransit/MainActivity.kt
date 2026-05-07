package com.example.halifaxtransit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.*
import com.example.halifaxtransit.screens.BusMapScreen
import com.example.halifaxtransit.screens.RoutesScreen
import com.example.halifaxtransit.screens.FavouritesScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.startGtfsUpdates()

        setContent {

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            LaunchedEffect(Unit) {
                viewModel.initDb(this@MainActivity)
            }

            Scaffold(
                bottomBar = {
                    NavigationBar {

                        NavigationBarItem(
                            selected = currentRoute == "map",
                            onClick = { navController.navigate("map") { launchSingleTop = true } },
                            label = { Text("Map") },
                            icon = { Image(painterResource(R.drawable.mapicon), null) }
                        )

                        NavigationBarItem(
                            selected = currentRoute == "routes",
                            onClick = { navController.navigate("routes") { launchSingleTop = true } },
                            label = { Text("Routes") },
                            icon = { Image(painterResource(R.drawable.listicon), null) }
                        )

                        NavigationBarItem(
                            selected = currentRoute == "favourites",
                            onClick = { navController.navigate("favourites") { launchSingleTop = true } },
                            label = { Text("Favourites") },
                            icon = { Image(painterResource(R.drawable.favouritesicon), null) }
                        )
                    }
                }
            ) { padding ->

                NavHost(
                    navController = navController,
                    startDestination = "map",
                    modifier = Modifier.padding(padding)
                ) {

                    composable("map") { BusMapScreen(viewModel) }
                    composable("routes") { RoutesScreen(viewModel) }
                    composable("favourites") { FavouritesScreen(viewModel) }

                }
            }
        }
    }
}
