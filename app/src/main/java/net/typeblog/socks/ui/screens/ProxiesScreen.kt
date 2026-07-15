package net.typeblog.socks.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.typeblog.socks.ui.components.ProxyCard
import net.typeblog.socks.ui.components.VpnButton
import net.typeblog.socks.ui.viewmodel.VpnViewModel
import net.typeblog.socks.util.ProfileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiesScreen(
    modifier: Modifier = Modifier,
    viewModel: VpnViewModel
) {
    Log.d("KiloProxyScreen", "ProxiesScreen composed")
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val activeProfileName by viewModel.activeProfileName.collectAsState()

    Log.d("KiloProxyScreen", "ProxiesScreen profiles=${profiles.size}, isRunning=$isRunning")

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<String?>(null) }
    var editTargetProfile by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add proxy")
            }
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No proxies configured",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap + to add your first proxy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(profiles, key = { it }) { profileName ->
                    val pm = remember { ProfileManager.getInstance(context) }
                    val profile = remember(profileName) { pm.getProfile(profileName) }
                    ProxyCard(
                        profileName = profileName,
                        server = profile?.getServer() ?: "",
                        port = profile?.getPort() ?: 0,
                        isConnected = isRunning && activeProfileName == profileName,
                        onClick = { selectedProfile = profileName }
                    )
                }
                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    // ── Proxy Detail Sheet ──
    selectedProfile?.let { name ->
        ProxyDetailSheet(
            profileName = name,
            isRunning = isRunning,
            isActiveProfile = activeProfileName == name,
            viewModel = viewModel,
            onDismiss = { selectedProfile = null },
            onEdit = {
                selectedProfile = null
                editTargetProfile = name
            }
        )
    }

    // ── Add/Edit Sheet ──
    when {
        showAddSheet -> {
            AddEditProxySheet(
                profileName = null,
                onDismiss = {
                    showAddSheet = false
                    viewModel.reloadProfiles(context)
                },
                onSaved = {
                    showAddSheet = false
                    viewModel.reloadProfiles(context)
                }
            )
        }
        editTargetProfile != null -> {
            AddEditProxySheet(
                profileName = editTargetProfile,
                onDismiss = {
                    editTargetProfile = null
                    viewModel.reloadProfiles(context)
                },
                onSaved = {
                    editTargetProfile = null
                    viewModel.reloadProfiles(context)
                }
            )
        }
    }
}

// ── Proxy Detail Bottom Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxyDetailSheet(
    profileName: String,
    isRunning: Boolean,
    isActiveProfile: Boolean,
    viewModel: VpnViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val pm = remember { ProfileManager.getInstance(context) }
    val profile = remember(profileName) { pm.getProfile(profileName) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onVpnPermissionResult(context)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // Status header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isThisConnected = isRunning && isActiveProfile
                val dotColor = if (isThisConnected) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .let { mod ->
                            if (isThisConnected) {
                                mod
                            } else mod
                        }
                ) {
                    Text(
                        text = if (isThisConnected) "●" else "○",
                        color = dotColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isThisConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isThisConnected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Detail rows
            DetailRow(label = "Profile", value = profileName)
            DetailRow(label = "Host", value = profile?.getServer() ?: "")
            DetailRow(label = "Port", value = profile?.getPort()?.toString() ?: "")
            DetailRow(
                label = "Auth",
                value = if (profile?.isUserPw() == true) "Username + Password" else "None"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // VPN start/stop button
            VpnButton(
                isRunning = isRunning && isActiveProfile,
                onStart = {
                    val intent = viewModel.prepareAndStartVpn(context, profileName)
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    }
                },
                onStop = { viewModel.stopVpn(context) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Edit button
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "Edit Proxy",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

// ── Add/Edit Proxy Bottom Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditProxySheet(
    profileName: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val isEdit = profileName != null

    // Form state
    var name by remember { mutableStateOf(profileName ?: "") }
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Load existing profile data for editing
    LaunchedEffect(profileName) {
        if (profileName != null) {
            try {
                val pm = ProfileManager.getInstance(context)
                val profile = pm.getProfile(profileName)
                if (profile != null) {
                    name = profile.getName()
                    host = profile.getServer()
                    portText = profile.getPort().toString()
                    username = profile.getUsername()
                    password = profile.getPassword()
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    // Validation
    val hostValid = host.trim().isNotEmpty()
    val portValid = portText.trim().toIntOrNull()?.let { it in 1..65535 } ?: false
    val userValid = true
    val passValid = true
    val allValid = hostValid && portValid && userValid && passValid && name.trim().isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (isEdit) "Edit Proxy" else "Add Proxy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Profile Name
            FormField(
                label = "Profile Name",
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. My Proxy",
                enabled = !isEdit
            )

            // Host field
            FormFieldWithValidation(
                label = "Host",
                value = host,
                onValueChange = { host = it },
                placeholder = "e.g. proxy.example.com",
                isValid = hostValid,
                showValidation = host.isNotEmpty()
            )

            // Port field
            FormFieldWithValidation(
                label = "Port",
                value = portText,
                onValueChange = { portText = it },
                placeholder = "e.g. 1080",
                isValid = portValid,
                showValidation = portText.isNotEmpty(),
                keyboardType = KeyboardType.Number
            )

            // Auth fields
            FormFieldWithValidation(
                label = "Username",
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                isValid = userValid,
                showValidation = username.isNotEmpty()
            )
            FormFieldWithValidation(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                isValid = passValid,
                showValidation = password.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Validation summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { mod ->
                        if (allValid) {
                            mod
                        } else mod
                    }
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (allValid) "✓ Ready to save" else "Fill all required fields",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (allValid) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel / Test / Save buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        scope.launch { sheetState.hide(); onDismiss() }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TestConnectionButton(
                    host = host.trim(),
                    portText = portText.trim(),
                    username = username.trim(),
                    password = password.trim()
                )
                Button(
                    onClick = {
                        saveProfile(
                            context = context,
                            profileName = profileName,
                            newName = name.trim(),
                            host = host.trim(),
                            portText = portText.trim(),
                            username = username.trim(),
                            password = password.trim()
                        )
                        scope.launch {
                            sheetState.hide()
                            onSaved()
                        }
                    },
                    enabled = allValid,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TestConnectionButton(
    host: String,
    portText: String,
    username: String,
    password: String,
    modifier: Modifier = Modifier
) {
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            testing = true
            testResult = null
            val port = portText.toIntOrNull()
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            Thread {
                val result = try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(host, port ?: 0), 5000)
                    socket.soTimeout = 5000
                    val ins = socket.getInputStream()
                    val outs = socket.getOutputStream()

                    // SOCKS5 auth method negotiation
                    outs.write(byteArrayOf(0x05, 0x01, 0x02.toByte()))
                    outs.flush()
                    val authResp = ByteArray(2)
                    ins.read(authResp)
                    if (authResp[0] != 0x05.toByte()) throw Exception("Not a SOCKS5 proxy")
                    if (authResp[1] == 0xff.toByte()) throw Exception("No acceptable auth method")

                    // Username/password auth (RFC 1929)
                    if (authResp[1] == 0x02.toByte()) {
                        val uBytes = username.toByteArray()
                        val pBytes = password.toByteArray()
                        val authReq = ByteArray(3 + uBytes.size + pBytes.size)
                        authReq[0] = 0x01
                        authReq[1] = uBytes.size.toByte()
                        System.arraycopy(uBytes, 0, authReq, 2, uBytes.size)
                        authReq[2 + uBytes.size] = pBytes.size.toByte()
                        System.arraycopy(pBytes, 0, authReq, 3 + uBytes.size, pBytes.size)
                        outs.write(authReq)
                        outs.flush()
                        val authResp2 = ByteArray(2)
                        ins.read(authResp2)
                        if (authResp2[1] != 0x00.toByte()) throw Exception("Auth failed")
                    }

                    // CONNECT to a test target
                    val testHost = "google.com"
                    val testPort = 80
                    val testHostBytes = testHost.toByteArray()
                    val connectReq = ByteArray(6 + testHostBytes.size)
                    connectReq[0] = 0x05
                    connectReq[1] = 0x01
                    connectReq[2] = 0x00
                    connectReq[3] = 0x03
                    connectReq[4] = testHostBytes.size.toByte()
                    System.arraycopy(testHostBytes, 0, connectReq, 5, testHostBytes.size)
                    connectReq[5 + testHostBytes.size] = (testPort shr 8).toByte()
                    connectReq[6 + testHostBytes.size] = testPort.toByte()
                    outs.write(connectReq)
                    outs.flush()
                    val connectResp = ByteArray(4)
                    ins.read(connectResp)
                    if (connectResp[1] != 0x00.toByte()) {
                        val errCodes = mapOf(
                            0x01 to "General SOCKS failure",
                            0x02 to "Connection not allowed",
                            0x03 to "Network unreachable",
                            0x04 to "Host unreachable",
                            0x05 to "Connection refused",
                            0x06 to "TTL expired",
                            0x07 to "Command not supported",
                            0x08 to "Address not supported"
                        )
                        val errMsg = errCodes[connectResp[1].toInt()] ?: "Error code ${connectResp[1].toInt()}"
                        throw Exception(errMsg)
                    }
                    // Consume the rest of the SOCKS response (BND.ADDR + BND.PORT)
                    val addrType = connectResp[3]
                    val remaining = when (addrType.toInt()) {
                        0x01 -> 4 + 2
                        0x04 -> 16 + 2
                        0x03 -> {
                            val lenByte = ByteArray(1)
                            ins.read(lenByte)
                            1 + lenByte[0].toInt() + 2
                        }
                        else -> throw Exception("Unknown address type")
                    }
                    var skipped = 0
                    while (skipped < remaining) {
                        val n = ins.read(ByteArray(remaining - skipped))
                        if (n < 0) throw Exception("Connection closed")
                        skipped += n
                    }
                    socket.close()
                    "✓ Proxy works"
                } catch (e: Exception) {
                    val msg = e.message ?: "Failed"
                    if (msg.length > 50) "✗ ${msg.take(50)}" else "✗ $msg"
                }
                mainHandler.post {
                    testResult = result
                    testing = false
                }
            }.start()
        },
        enabled = host.isNotEmpty() && portText.toIntOrNull() != null && !testing,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        if (testing) {
            Text("Testing...", fontSize = 13.sp)
        } else {
            Text(
                text = testResult ?: "Test Connection",
                fontSize = 13.sp,
                color = when {
                    testResult?.startsWith("✓") == true -> MaterialTheme.colorScheme.tertiary
                    testResult?.startsWith("✗") == true -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true
) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}

@Composable
private fun FormFieldWithValidation(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isValid: Boolean,
    showValidation: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = showValidation && !isValid,
        suffix = {
            if (showValidation) {
                Text(
                    text = if (isValid) "✓" else "✗",
                    color = if (isValid) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun saveProfile(
    context: android.content.Context,
    profileName: String?,
    newName: String,
    host: String,
    portText: String,
    username: String,
    password: String
) {
    try {
        val pm = ProfileManager.getInstance(context)
        val port = portText.toIntOrNull() ?: 1080

        if (profileName != null) {
            // Edit existing profile
            val profile = pm.getProfile(profileName) ?: return
            profile.setServer(host)
            profile.setPort(port)
            profile.setIsUserpw(true)
            profile.setUsername(username)
            profile.setPassword(password)
        } else {
            // Add new profile
            val profile = pm.addProfile(newName) ?: return
            profile.setServer(host)
            profile.setPort(port)
            profile.setIsUserpw(true)
            profile.setUsername(username)
            profile.setPassword(password)
        }
    } catch (_: Exception) {
        // Ignore
    }
}
