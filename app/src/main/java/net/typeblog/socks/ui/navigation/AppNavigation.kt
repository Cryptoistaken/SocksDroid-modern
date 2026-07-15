package net.typeblog.socks.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import net.typeblog.socks.ui.screens.ProxiesScreen
import net.typeblog.socks.ui.screens.StatusScreen
import net.typeblog.socks.ui.screens.SettingsScreen
import net.typeblog.socks.ui.screens.SplitTunnelingScreen
import net.typeblog.socks.ui.viewmodel.VpnViewModel

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
    BottomNavItem(Screen.Proxies, Icons.Filled.Language, "Proxies"),
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
    val vpnViewModel: VpnViewModel = viewModel()

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
                Log.d("KiloProxyNav", "Navigated to ProxiesScreen")
                ProxiesScreen(viewModel = vpnViewModel)
            }
            composable(Screen.Status.route) {
                Log.d("KiloProxyNav", "Navigated to StatusScreen")
                StatusScreen(viewModel = vpnViewModel)
            }
            composable(Screen.Settings.route) {
                Log.d("KiloProxyNav", "Navigated to SettingsScreen")
                SettingsScreen(onNavigateToSplitTunneling = {
                    navController.navigate(Screen.SplitTunneling.route)
                })
            }
            composable(Screen.SplitTunneling.route) {
                Log.d("KiloProxyNav", "Navigated to SplitTunnelingScreen")
                SplitTunnelingScreen(onNavigateBack = {
                    navController.popBackStack()
                })
            }
        }
    }
}
