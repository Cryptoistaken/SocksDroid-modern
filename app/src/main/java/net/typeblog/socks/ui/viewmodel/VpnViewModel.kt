package net.typeblog.socks.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.typeblog.socks.IVpnService
import net.typeblog.socks.SocksVpnService
import net.typeblog.socks.util.ProfileManager
import net.typeblog.socks.util.Utility

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentIp = MutableStateFlow<String?>(null)
    val currentIp: StateFlow<String?> = _currentIp.asStateFlow()

    private val _countryCode = MutableStateFlow<String?>(null)
    val countryCode: StateFlow<String?> = _countryCode.asStateFlow()

    private val _connectedSince = MutableStateFlow(0L)
    val connectedSince: StateFlow<Long> = _connectedSince.asStateFlow()

    private val _realIp = MutableStateFlow<String?>(null)
    val realIp: StateFlow<String?> = _realIp.asStateFlow()

    private val _realCountryCode = MutableStateFlow<String?>(null)
    val realCountryCode: StateFlow<String?> = _realCountryCode.asStateFlow()

    private val _profiles = MutableStateFlow<List<String>>(emptyList())
    val profiles: StateFlow<List<String>> = _profiles.asStateFlow()

    private val _activeProfileName = MutableStateFlow<String?>(null)
    val activeProfileName: StateFlow<String?> = _activeProfileName.asStateFlow()

    private var _pendingProfile = MutableStateFlow<String?>(null)

    private var vpnService: IVpnService? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            vpnService = IVpnService.Stub.asInterface(service)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vpnService = null
            bound = false
        }
    }

    init {
        Log.d("KiloProxyVM", "VpnViewModel init - instance ${hashCode()}")
        val app = getApplication<Application>()
        bindToService(app)
        loadProfiles(app)
        checkPublicIp()
        startPolling()
    }

    private fun bindToService(context: Context) {
        val intent = Intent(context, SocksVpnService::class.java)
        try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (_: Exception) {
            // Service not available or binding failed
        }
    }

    private fun loadProfiles(context: Context) {
        try {
            val pm = ProfileManager.getInstance(context)
            pm.reload()
            val allProfiles = pm.getProfiles().toList()
            // Skip first element (default placeholder), return only user-created profiles
            _profiles.value = allProfiles.drop(1)
            Log.d("KiloProxyVM", "loadProfiles: ${allProfiles.size} total, ${allProfiles.drop(1).size} user profiles")
        } catch (e: Exception) {
            Log.e("KiloProxyVM", "loadProfiles failed: ${e.message}")
            _profiles.value = emptyList()
        }
    }

    fun reloadProfiles(context: Context) {
        loadProfiles(context)
    }

    private fun checkPublicIp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = Utility.checkPublicIp()
                if (info != null) {
                    _realIp.value = info.ip
                    _realCountryCode.value = info.countryCode
                }
            } catch (_: Exception) {
                // Ignore network errors
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                if (bound && vpnService != null) {
                    try {
                        val running = vpnService!!.isRunning
                        _isRunning.value = running
                        if (running) {
                            _currentIp.value = vpnService!!.currentIp.ifEmpty { null }
                            _countryCode.value = vpnService!!.countryCode.ifEmpty { null }
                            _connectedSince.value = vpnService!!.connectedSince
                        } else {
                            _currentIp.value = null
                            _countryCode.value = null
                            _connectedSince.value = 0L
                            _activeProfileName.value = null
                        }
                    } catch (_: Exception) {
                        clearState()
                    }
                } else {
                    clearState()
                }
                delay(500L)
            }
        }
    }

    private fun clearState() {
        _isRunning.value = false
        _currentIp.value = null
        _countryCode.value = null
        _connectedSince.value = 0L
        _activeProfileName.value = null
    }

    fun startVpn(context: Context, profileName: String) {
        Log.d("KiloProxyVM", "startVpn called for profile: $profileName")
        viewModelScope.launch {
            try {
                val pm = ProfileManager.getInstance(context)
                val profile = pm.getProfile(profileName) ?: run {
                    Log.e("KiloProxyVM", "startVpn: profile not found: $profileName")
                    return@launch
                }
                Utility.startVpn(context, profile)
                _activeProfileName.value = profileName
                pm.switchDefault(profileName)
                Log.d("KiloProxyVM", "startVpn succeeded for: $profileName")
            } catch (e: Exception) {
                Log.e("KiloProxyVM", "startVpn failed: ${e.message}")
            }
        }
    }

    fun stopVpn(context: Context) {
        Log.d("KiloProxyVM", "stopVpn called")
        viewModelScope.launch {
            if (bound && vpnService != null) {
                try {
                    vpnService!!.stop()
                    Log.d("KiloProxyVM", "stopVpn succeeded")
                } catch (e: Exception) {
                    Log.e("KiloProxyVM", "stopVpn failed: ${e.message}")
                }
            } else {
                Log.w("KiloProxyVM", "stopVpn: service not bound")
            }
        }
    }

    /**
     * Prepare VPN connection: if VPN permission is already granted, start directly;
     * otherwise store the pending profile and return the permission intent for the caller to launch.
     */
    fun prepareAndStartVpn(context: Context, profileName: String): Intent? {
        val intent = VpnService.prepare(context)
        if (intent == null) {
            // Optimistically set active profile before VPN starts
            _activeProfileName.value = profileName
            startVpn(context, profileName)
            return null
        }
        // Store pending even if permission needed
        _pendingProfile.value = profileName
        return intent
    }

    /**
     * Called after the user grants (or dismisses) the VPN permission dialog.
     * If permission was granted (RESULT_OK already checked by caller), starts the pending profile.
     */
    fun onVpnPermissionResult(context: Context) {
        val profile = _pendingProfile.value ?: return
        _pendingProfile.value = null
        _activeProfileName.value = profile // immediate feedback
        startVpn(context, profile)
    }

    fun getProfileIpInfo(profileName: String): String {
        val app = getApplication<Application>()
        return try {
            val pm = ProfileManager.getInstance(app)
            val profile = pm.getProfile(profileName) ?: return ""
            "${profile.getServer()}:${profile.getPort()}"
        } catch (_: Exception) {
            ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (_: Exception) {
            // Ignore
        }
        bound = false
        vpnService = null
    }
}
