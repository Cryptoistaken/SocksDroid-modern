package net.typeblog.socks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import net.typeblog.socks.ui.components.AppToggleItem
import net.typeblog.socks.util.Constants.PREF_ADV_APP_LIST

/**
 * Represents an installed app with its display name and package name.
 */
private data class InstalledApp(
    val name: String,
    val packageName: String
)

/**
 * Split tunneling configuration screen.
 *
 * Features a segmented control (Allowed / Blocked) and a list of installed
 * apps (represented by a static demo list) with per-app toggle rows.
 * Toggle state is persisted to [PREF_ADV_APP_LIST] via SharedPreferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelingScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val packageManager = context.packageManager

    // Load persisted app list into a set
    val persistedList = remember {
        prefs.getString(PREF_ADV_APP_LIST, "")?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()
    }

    // Real installed launcher apps, loaded asynchronously
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

    LaunchedEffect(Unit) {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_INSTALLED != 0 }
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                InstalledApp(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = it.packageName
                )
            }
            .sortedBy { it.name.lowercase() }
        installedApps = apps
    }

    // Toggle state for each app — true = in the VPN list, false = not
    val toggleStates = remember { mutableStateMapOf<String, Boolean>() }

    // Sync persisted toggle states once real apps are loaded
    LaunchedEffect(installedApps) {
        if (installedApps.isNotEmpty()) {
            installedApps.forEach { app ->
                if (!toggleStates.containsKey(app.packageName)) {
                    toggleStates[app.packageName] = persistedList.contains(app.packageName)
                }
            }
        }
    }

    // 0 = Allowed tab, 1 = Blocked tab
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Split Tunneling") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Segmented control ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SegmentedTab(
                    text = "Allowed",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                SegmentedTab(
                    text = "Blocked",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (selectedTab == 0)
                    "Apps that will route through the VPN"
                else
                    "Apps that will bypass the VPN",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )

            // ── App list ──
            // Filter: Allowed tab shows toggled-on apps; Blocked tab shows toggled-off apps
            val filteredApps = installedApps.filter { app ->
                val isOn = toggleStates[app.packageName] == true
                if (selectedTab == 0) isOn else !isOn
            }

            if (installedApps.isEmpty()) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "Loading apps…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                )
            } else if (filteredApps.isEmpty()) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = if (selectedTab == 0)
                        "No apps routed through VPN"
                    else
                        "No apps bypassing VPN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                )
            } else {
                LazyColumn {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isOn = toggleStates[app.packageName] == true
                        AppToggleItem(
                            appName = app.name,
                            packageName = app.packageName,
                            isAllowed = isOn,
                            onToggle = { newValue ->
                                toggleStates[app.packageName] = newValue
                                // Persist to SharedPreferences
                                val updatedList = toggleStates
                                    .filterValues { it }
                                    .keys
                                    .joinToString("\n")
                                prefs.edit()
                                    .putString(PREF_ADV_APP_LIST, updatedList)
                                    .apply()
                            }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Segmented tab composable
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun SegmentedTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
