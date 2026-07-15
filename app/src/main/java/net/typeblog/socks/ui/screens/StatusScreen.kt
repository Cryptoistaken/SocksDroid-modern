package net.typeblog.socks.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.typeblog.socks.R
import net.typeblog.socks.ui.components.ConnectionCard
import net.typeblog.socks.ui.components.VpnButton
import net.typeblog.socks.ui.viewmodel.VpnViewModel
import net.typeblog.socks.util.ProfileManager

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    viewModel: VpnViewModel
) {
    Log.d("KiloProxyScreen", "StatusScreen composed")
    val context = LocalContext.current
    val isRunning by viewModel.isRunning.collectAsState()
    val currentIp by viewModel.currentIp.collectAsState()
    val countryCode by viewModel.countryCode.collectAsState()
    val connectedSince by viewModel.connectedSince.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileName by viewModel.activeProfileName.collectAsState()

    Log.d("KiloProxyScreen", "StatusScreen isRunning=$isRunning profiles=${profiles.size}")

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onVpnPermissionResult(context)
        }
    }

    // Determine server name from active profile or default
    val serverName = remember(activeProfileName, profiles) {
        if (activeProfileName != null) {
            try {
                val pm = ProfileManager.getInstance(context)
                val p = pm.getProfile(activeProfileName!!)
                if (p != null) "${p.getServer()}:${p.getPort()}" else ""
            } catch (_: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {
        // KiloProxy header – logo left + "KiloProxy" text
        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val logoSrc = if (isDarkTheme) R.drawable.logo_dark else R.drawable.logo_light

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = logoSrc),
                contentDescription = "KiloProxy",
                modifier = Modifier.height(40.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "KiloProxy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Connection status card
        ConnectionCard(
            isConnected = isRunning,
            ip = currentIp,
            countryCode = countryCode,
            serverName = serverName,
            connectedSince = connectedSince,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // VPN start/stop button
        if (profiles.isEmpty()) {
            // No profiles configured
            Text(
                text = "Add a proxy profile first",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            VpnButton(
                isRunning = isRunning,
                    onStart = {
                        // Use the active profile, or default to first profile
                        val targetProfile = activeProfileName ?: profiles.firstOrNull()
                        if (targetProfile != null) {
                            val intent = viewModel.prepareAndStartVpn(context, targetProfile)
                            if (intent != null) {
                                vpnPermissionLauncher.launch(intent)
                            }
                        }
                    },
                onStop = {
                    viewModel.stopVpn(context)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
