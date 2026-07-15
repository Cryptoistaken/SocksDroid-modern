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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
    var showThemeDialog by remember { mutableStateOf(false) }

    val themeLabel = when (themeMode) {
        "dark" -> "Dark"
        "system" -> "System"
        else -> "Light"
    }

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
                SettingsItem(
                    icon = Icons.Outlined.DarkMode,
                    label = "Theme Mode",
                    value = themeLabel,
                    onClick = { showThemeDialog = true }
                )
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
            TextButton(onClick = { onConfirm(selected) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
