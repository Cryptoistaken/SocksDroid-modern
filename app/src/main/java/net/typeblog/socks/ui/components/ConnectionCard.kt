package net.typeblog.socks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.typeblog.socks.util.Utility

@Composable
fun ConnectionCard(
    isConnected: Boolean,
    ip: String?,
    countryCode: String?,
    serverName: String,
    connectedSince: Long,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val durationFormatted = if (isConnected && connectedSince > 0L) {
        val diff = currentTime - connectedSince
        val totalSeconds = diff / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    } else {
        "—"
    }

    val shieldColor = if (isConnected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val shieldBackground = if (isConnected) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(shieldBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = if (isConnected) "Connected" else "Disconnected",
                    tint = shieldColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val badgeBackground = if (isConnected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val badgeColor = if (isConnected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(badgeBackground)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val infoBackground = MaterialTheme.colorScheme.surfaceVariant

            InfoRow(
                label = "Your IP",
                value = buildIpDisplay(ip, countryCode),
                isIp = true,
                background = infoBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            InfoRow(
                label = "Server",
                value = serverName.ifEmpty { "—" },
                background = infoBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            InfoRow(
                label = "Uptime",
                value = durationFormatted,
                background = infoBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            IconButton(
                onClick = { if (isConnected) onStopClick() else onStartClick() },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isConnected) "Stop" else "Start",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isIp: Boolean = false,
    background: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = if (isIp) 15.sp else 12.sp,
            fontWeight = if (isIp) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun buildIpDisplay(ip: String?, countryCode: String?): String {
    if (ip.isNullOrEmpty()) return "—"
    val flag = if (!countryCode.isNullOrEmpty()) {
        try {
            Utility.countryCodeToFlag(countryCode)
        } catch (_: Exception) {
            ""
        }
    } else {
        ""
    }
    val cc = countryCode ?: ""
    return buildString {
        if (flag.isNotEmpty()) append("$flag ")
        if (cc.isNotEmpty()) append("$cc ")
        append(ip)
    }.trim()
}
