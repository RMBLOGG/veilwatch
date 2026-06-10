package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.ui.VeilwatchViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    val navController = rememberNavController()
    val viewModel: VeilwatchViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide navigation bar on Detail or Streaming Player screens for cinematic immersion
    val showNavPill = currentRoute in listOf("home", "browse", "watchlist", "profile")

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        bottomBar = {
            if (showNavPill) {
                FloatingPillNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute ?: "home"
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing // Prevent camera notch conflicts
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showNavPill) 40.dp else 0.dp) // Reserve overlay spacing for pill float
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { animeId -> navController.navigate("detail/$animeId") },
                    onNavigateToDiscover = { navController.navigate("browse") },
                    onNavigateToWatch = { animeId, ep -> navController.navigate("watch/$animeId/$ep") }
                )
            }

            composable("browse") {
                BrowseScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { animeId -> navController.navigate("detail/$animeId") }
                )
            }

            composable("watchlist") {
                WatchlistScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { animeId -> navController.navigate("detail/$animeId") },
                    onBrowseClick = { navController.navigate("browse") }
                )
            }

            composable("profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { animeId -> navController.navigate("detail/$animeId") }
                )
            }

            composable("detail/{animeId}") { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId")?.toIntOrNull() ?: 1
                DetailScreen(
                    animeId = animeId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { id, ep -> navController.navigate("watch/$id/$ep") }
                )
            }

            composable("watch/{animeId}/{episodeNumber}") { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId")?.toIntOrNull() ?: 1
                val episodeNumber = backStackEntry.arguments?.getString("episodeNumber")?.toIntOrNull() ?: 1
                WatchScreen(
                    animeId = animeId,
                    episodeNum = episodeNumber,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Floating gear shortcut settings button visible on Home
        if (currentRoute == "home") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurface.copy(alpha = 0.8f))
                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingPillNavigationBar(
    navController: NavHostController,
    currentRoute: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(100))
                .background(DarkSurface.copy(alpha = 0.85f))
                .border(2.dp, PrimaryAccent.copy(alpha = 0.3f), RoundedCornerShape(100))
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("bottom_nav_pill"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Pair("home", Icons.Default.Home),
                Pair("browse", Icons.Default.Search),
                Pair("watchlist", Icons.Default.Bookmark),
                Pair("profile", Icons.Default.Person)
            )

            items.forEach { (route, icon) ->
                val selected = currentRoute == route
                val backgroundAlpha by animateFloatAsState(targetValue = if (selected) 0.15f else 0.0f)
                val iconColor by animateColorAsState(targetValue = if (selected) PrimaryAccent else Color.White.copy(alpha = 0.6f))
                val scale by animateFloatAsState(targetValue = if (selected) 1.15f else 1.0f)

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryAccent.copy(alpha = backgroundAlpha))
                        .clickable {
                            if (!selected) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .testTag("nav_item_$route"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = route.uppercase(),
                        tint = iconColor,
                        modifier = Modifier
                            .scale(scale)
                            .size(22.dp)
                    )
                }
            }
        }
    }
}
