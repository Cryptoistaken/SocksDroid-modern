package net.typeblog.socks.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import net.typeblog.socks.BuildConfig
import net.typeblog.socks.ui.components.SettingsItem
import net.typeblog.socks.util.Constants.PREF_ADV_AUTO_CONNECT
import net.typeblog.socks.util.Constants.PREF_ADV_DNS
import net.typeblog.socks.util.Constants.PREF_ADV_DNS_PORT
import net.typeblog.socks.util.Constants.PREF_ADV_ROUTE
import net.typeblog.socks.util.Constants.PREF_AUTO_STOP
import net.typeblog.socks.util.Constants.PREF_CONNECTIVITY_CHECK
import net.typeblog.socks.util.Constants.PREF_DYNAMIC_COLORS
import net.typeblog.socks.util.Constants.PREF_IPV6_PROXY
import net.typeblog.socks.util.Constants.PREF_THEME_MODE
import net.typeblog.socks.util.Constants.PREF_UDP_PROXY

/**
 * Settings screen with grouped preference items.
 *
 * Sections: Appearance, Connection, Routing, Advanced, Split Tunneling, About.
 * Uses [SettingsItem] composables inside rounded outlined groups.
 */
@Composable
fun SettingsScreen(
    onNavigateToSplitTunneling: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // ── Preference state ──
    var themeMode by remember {
        mutableStateOf(prefs.getString(PREF_THEME_MODE, "light") ?: "light")
    }
    var dynamicColors by remember {
        mutableStateOf(prefs.getBoolean(PREF_DYNAMIC_COLORS, true))
    }
    var autoConnect by remember {
        mutableStateOf(prefs.getBoolean(PREF_ADV_AUTO_CONNECT, false))
    }
    var connectivityCheck by remember {
        mutableStateOf(prefs.getBoolean(PREF_CONNECTIVITY_CHECK, true))
    }
    var udpProxy by remember {
        mutableStateOf(prefs.getBoolean(PREF_UDP_PROXY, false))
    }
    var ipv6Proxy by remember {
        mutableStateOf(prefs.getBoolean(PREF_IPV6_PROXY, false))
    }
    var routePref by remember {
        mutableStateOf(prefs.getString(PREF_ADV_ROUTE, "all") ?: "all")
    }
    var dnsServer by remember {
        mutableStateOf(prefs.getString(PREF_ADV_DNS, "8.8.8.8") ?: "8.8.8.8")
    }
    var dnsPort by remember {
        mutableStateOf(prefs.getInt(PREF_ADV_DNS_PORT, 53))
    }
    var autoStop by remember {
        mutableStateOf(prefs.getBoolean(PREF_AUTO_STOP, false))
    }

    // ── Dialog state ──
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRouteDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }

    // ── Derived display labels ──
    val themeLabel = when (themeMode) {
        "dark" -> "Dark"
        "system" -> "System"
        else -> "Light"
    }

    val routeLabel = when (routePref) {
        "chn" -> "Bypass Chinese IPs"
        "ru" -> "Bypass Russian IPs"
        "ru_chn" -> "Bypass RU & CN IPs"
        else -> "All Traffic"
    }

    val dnsDisplay = "$dnsServer:$dnsPort"

    // ── Helper lambdas ──
    fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun saveInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ════════════════════════════════════════════════════════════════
        // Appearance
        // ════════════════════════════════════════════════════════════════
        item {
            SectionTitle(text = "Appearance")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.DarkMode,
                    label = "Theme Mode",
                    value = themeLabel,
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    label = "Dynamic Colors",
                    description = "Apply colors from your wallpaper (Android 12+)",
                    trailing = {
                        Switch(
                            checked = dynamicColors,
                            onCheckedChange = {
                                dynamicColors = it
                                saveBoolean(PREF_DYNAMIC_COLORS, it)
                            }
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Language,
                    label = "Language",
                    value = "English",
                    onClick = { /* placeholder — i18n is a stretch goal */ }
                )
            }
        }

        // ════════════════════════════════════════════════════════════════
        // Connection
        // ════════════════════════════════════════════════════════════════
        item {
            SectionTitle(text = "Connection")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Shield,
                    label = "Auto-connect",
                    description = "Connect on boot",
                    trailing = {
                        Switch(
                            checked = autoConnect,
                            onCheckedChange = {
                                autoConnect = it
                                saveBoolean(PREF_ADV_AUTO_CONNECT, it)
                            }
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.NetworkCheck,
                    label = "Connectivity Check",
                    description = "Verifies proxy reachability",
                    trailing = {
                        Switch(
                            checked = connectivityCheck,
                            onCheckedChange = {
                                connectivityCheck = it
                                saveBoolean(PREF_CONNECTIVITY_CHECK, it)
                            }
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Router,
                    label = "UDP Proxy",
                    description = "Forward UDP packets to UDPGW",
                    trailing = {
                        Switch(
                            checked = udpProxy,
                            onCheckedChange = {
                                udpProxy = it
                                saveBoolean(PREF_UDP_PROXY, it)
                            }
                        )
                    }
                )
                SettingsItem(
                    icon = Icons.Outlined.Public,
                    label = "IPv6 Proxy",
                    description = "Enable IPv6 forwarding",
                    trailing = {
                        Switch(
                            checked = ipv6Proxy,
                            onCheckedChange = {
                                ipv6Proxy = it
                                saveBoolean(PREF_IPV6_PROXY, it)
                            }
                        )
                    }
                )
            }
        }

        // ════════════════════════════════════════════════════════════════
        // Routing
        // ════════════════════════════════════════════════════════════════
        item {
            SectionTitle(text = "Routing")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.AltRoute,
                    label = "Route Preference",
                    value = routeLabel,
                    onClick = { showRouteDialog = true }
                )
                SettingsItem(
                    icon = Icons.Outlined.Dns,
                    label = "DNS Server",
                    value = dnsDisplay,
                    onClick = { showDnsDialog = true }
                )
            }
        }

        // ════════════════════════════════════════════════════════════════
        // Advanced
        // ════════════════════════════════════════════════════════════════
        item {
            SectionTitle(text = "Advanced")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Lock,
                    label = "Auto-stop on screen off",
                    description = "Stop VPN when screen turns off",
                    trailing = {
                        Switch(
                            checked = autoStop,
                            onCheckedChange = {
                                autoStop = it
                                saveBoolean(PREF_AUTO_STOP, it)
                            }
                        )
                    }
                )
            }
        }

        // ════════════════════════════════════════════════════════════════
        // Split Tunneling
        // ════════════════════════════════════════════════════════════════
        item {
            SectionTitle(text = "Split Tunneling")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Apps,
                    label = "Configure apps",
                    description = "Choose which apps route through the VPN or bypass it",
                    value = null,
                    onClick = onNavigateToSplitTunneling
                )
            }
        }

        // ════════════════════════════════════════════════════════════════
        // About
        // ════════════════════════════════════════════════════════════════
        item {
            SectionTitle(text = "About")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    label = "Version",
                    value = BuildConfig.VERSION_NAME
                )
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    label = "License",
                    value = "GPL v3"
                )
            }
        }

        // Bottom spacer so content isnt cut off by nav bar
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // ── Dialogs ──

    if (showThemeDialog) {
        ThemePickerDialog(
            currentValue = themeMode,
            onDismiss = { showThemeDialog = false },
            onConfirm = { value ->
                themeMode = value
                saveString(PREF_THEME_MODE, value)
                showThemeDialog = false
            }
        )
    }

    if (showRouteDialog) {
        RoutePickerDialog(
            currentValue = routePref,
            onDismiss = { showRouteDialog = false },
            onConfirm = { value ->
                routePref = value
                saveString(PREF_ADV_ROUTE, value)
                showRouteDialog = false
            }
        )
    }

    if (showDnsDialog) {
        DnsEditDialog(
            currentServer = dnsServer,
            currentPort = dnsPort,
            onDismiss = { showDnsDialog = false },
            onConfirm = { server, port ->
                dnsServer = server
                dnsPort = port
                saveString(PREF_ADV_DNS, server)
                saveInt(PREF_ADV_DNS_PORT, port)
                showDnsDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Section helper composables
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
    )
}

/**
 * Wraps children in a rounded outlined Surface, matching the
 * `.settings-group` style from the design mockup.
 */
@Composable
private fun SettingsGroup(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ThemePickerDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val options = listOf("light" to "Light", "dark" to "Dark", "system" to "System")
    var selected by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme Mode") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (value == selected) {
                            Text(
                                text = "\u2713",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RoutePickerDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val options = listOf(
        "all" to "All Traffic",
        "chn" to "Bypass Chinese IPs",
        "ru" to "Bypass Russian IPs",
        "ru_chn" to "Bypass RU & CN IPs"
    )
    var selected by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Route Preference") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (value == selected) {
                            Text(
                                text = "\u2713",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DnsEditDialog(
    currentServer: String,
    currentPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var server by remember { mutableStateOf(currentServer) }
    var port by remember { mutableStateOf(currentPort.toString()) }
    var portError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("DNS Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("DNS Server") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        portError = it.toIntOrNull() !in 1..65535 && it.isNotEmpty()
                    },
                    label = { Text("Port (TCP)") },
                    singleLine = true,
                    isError = portError,
                    supportingText = if (portError) {
                        { Text("Port must be 1–65535") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val portNum = port.toIntOrNull()
                    if (portNum != null && portNum in 1..65535 && server.isNotBlank()) {
                        onConfirm(server, portNum)
                    } else {
                        portError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
