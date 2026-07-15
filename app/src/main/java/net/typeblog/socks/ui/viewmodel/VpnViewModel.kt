package net.typeblog.socks.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
            // Skip first element (default placeholder), return only user-created profiles
            _profiles.value = pm.getProfiles().toList().drop(1)
        } catch (_: Exception) {
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
                delay(1000L)
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
        viewModelScope.launch {
            try {
                val pm = ProfileManager.getInstance(context)
                val profile = pm.getProfile(profileName) ?: return@launch
                Utility.startVpn(context, profile)
                _activeProfileName.value = profileName
                pm.switchDefault(profileName)
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    fun stopVpn(context: Context) {
        viewModelScope.launch {
            if (bound && vpnService != null) {
                try {
                    vpnService!!.stop()
                } catch (_: Exception) {
                    // Service unreachable
                }
            }
        }
    }

    fun getProfileIpInfo(profileName: String): String {
        val app = getApplication<Application>()
        return try {
            val pm = ProfileManager.getInstance(app)
            val profile = pm.getProfile(profileName) ?: return ""
            "${profile.server}:${profile.port}"
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
