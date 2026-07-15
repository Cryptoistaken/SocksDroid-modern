package net.typeblog.socks.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Globe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    data object Proxies : Screen("proxies")
    data object Status : Screen("status")
    data object Settings : Screen("settings")
    data object SplitTunneling : Screen("split_tunneling")
}

private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Proxies, Icons.Filled.Globe, "Proxies"),
    BottomNavItem(Screen.Status, Icons.Filled.Security, "Status"),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings, "Settings")
)

private val bottomNavRoutes = bottomNavItems.map { it.screen.route }.toSet()

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Proxies.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Proxies.route) {
                ProxiesScreenPlaceholder()
            }
            composable(Screen.Status.route) {
                StatusScreenPlaceholder()
            }
            composable(Screen.Settings.route) {
                SettingsScreenPlaceholder(onNavigateToSplitTunneling = {
                    navController.navigate(Screen.SplitTunneling.route)
                })
            }
            composable(Screen.SplitTunneling.route) {
                SplitTunnelingScreenPlaceholder(onBack = {
                    navController.popBackStack()
                })
            }
        }
    }
}

// Placeholder screens — will be replaced by Agent 2 and Agent 3

@Composable
private fun ProxiesScreenPlaceholder() {
    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "Proxies",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun StatusScreenPlaceholder() {
    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun SettingsScreenPlaceholder(onNavigateToSplitTunneling: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun SplitTunnelingScreenPlaceholder(onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "Split Tunneling",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
