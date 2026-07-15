package net.typeblog.socks.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import net.typeblog.socks.ui.components.SettingsItem
import net.typeblog.socks.util.Constants.PREF_NOTIFICATION_CONTROL
import net.typeblog.socks.util.Constants.PREF_THEME_MODE

@Composable
fun SettingsScreen(
    onNavigateToSplitTunneling: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var themeMode by remember {
        mutableStateOf(prefs.getString(PREF_THEME_MODE, "light") ?: "light")
    }
    var notificationControl by remember {
        mutableStateOf(prefs.getBoolean(PREF_NOTIFICATION_CONTROL, false))
    }
    var showThemeMenu by remember { mutableStateOf(false) }

    val themeLabel = when (themeMode) {
        "dark" -> "Dark"
        "system" -> "System"
        else -> "Light"
    }

    val themeOptions = listOf("light" to "Light", "dark" to "Dark", "system" to "System")

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            notificationControl = true
            prefs.edit().putBoolean(PREF_NOTIFICATION_CONTROL, true).apply()
        }
    }

    fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ═══ Appearance ═══
        item {
            SectionTitle(text = "Appearance")
            SettingsGroup {
                BoxWithDropdown {
                    SettingsItem(
                        icon = Icons.Outlined.DarkMode,
                        label = "Theme Mode",
                        value = themeLabel,
                        onClick = { showThemeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        themeOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(text = label, modifier = Modifier.weight(1f))
                                        if (value == themeMode) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    themeMode = value
                                    saveString(PREF_THEME_MODE, value)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ═══ Controls ═══
        item {
            SectionTitle(text = "Controls")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Notifications,
                    label = "Notification Quick Control",
                    description = "VPN control button in notification",
                    trailing = {
                        Switch(
                            checked = notificationControl,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val perm = Manifest.permission.POST_NOTIFICATIONS
                                    if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(perm)
                                        return@Switch
                                    }
                                }
                                notificationControl = enabled
                                saveBoolean(PREF_NOTIFICATION_CONTROL, enabled)
                            }
                        )
                    }
                )
            }
        }

        // ═══ Split Tunneling ═══
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

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun BoxWithDropdown(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        content()
    }
}
