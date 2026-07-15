package net.typeblog.socks.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VpnButton(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isRunning) {
        // Stop VPN — red outlined button
        OutlinedButton(
            onClick = onStop,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Text(
                text = "Stop VPN",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        // Start VPN — filled primary button
        Button(
            onClick = onStart,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Start VPN",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
