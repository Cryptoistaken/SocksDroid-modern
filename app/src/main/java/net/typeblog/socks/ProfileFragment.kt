package net.typeblog.socks

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceManager
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceFragmentCompat

import com.google.android.material.materialswitch.MaterialSwitch

import net.typeblog.socks.util.Profile
import net.typeblog.socks.util.ProfileManager
import net.typeblog.socks.util.Utility
import net.typeblog.socks.util.Constants.*

class ProfileFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var mManager: ProfileManager
    private var mProfile: Profile? = null

    private var mSwitch: MaterialSwitch? = null
    private var mRunning = false
    private var mStarting = false
    private var mStopping = false
    private var mBinder: IVpnService? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mBinder = IVpnService.Stub.asInterface(service)

            try {
                mRunning = mBinder!!.isRunning
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check running state", e)
            }

            if (mRunning) {
                updateState()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBinder = null
        }
    }

    private val mStateRunnable = Runnable {
        updateState()
        mSwitch?.postDelayed(this, 1000)
    }

    private val vpnPrepareLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Utility.startVpn(requireActivity(), mProfile!!)
            checkState()
        }
    }

    // ---- Preference fields ----
    private lateinit var mPrefProfile: ListPreference
    private lateinit var mPrefRoutes: ListPreference
    private lateinit var mPrefDnsPresets: ListPreference
    private lateinit var mPrefServer: EditTextPreference
    private lateinit var mPrefPort: EditTextPreference
    private lateinit var mPrefUsername: EditTextPreference
    private lateinit var mPrefPassword: EditTextPreference
    private lateinit var mPrefDns: EditTextPreference
    private lateinit var mPrefDnsPort: EditTextPreference
    private lateinit var mPrefAppList: EditTextPreference
    private lateinit var mPrefUDPGW: EditTextPreference
    private lateinit var mPrefUserpw: CheckBoxPreference
    private lateinit var mPrefPerApp: CheckBoxPreference
    private lateinit var mPrefAppBypass: CheckBoxPreference
    private lateinit var mPrefIPv6: CheckBoxPreference
    private lateinit var mPrefUDP: CheckBoxPreference
    private lateinit var mPrefAuto: CheckBoxPreference
    private var mPrefDynamic: CheckBoxPreference? = null
    private lateinit var mPrefAdd: Preference
    private lateinit var mPrefDel: Preference
    private lateinit var mPrefAppSelector: Preference

    // ---- Lifecycle ----

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManager = ProfileManager.getInstance(requireActivity().applicationContext)
        initPreferences()
        reload()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, null)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSwitch = requireActivity().findViewById(R.id.switch_action_button)
        mSwitch?.setOnCheckedChangeListener(this)
        mSwitch?.postDelayed(mStateRunnable, 1000)
        checkState()
    }

    override fun onResume() {
        super.onResume()
        mSwitch?.postDelayed(mStateRunnable, 1000)
    }

    override fun onPause() {
        super.onPause()
        mSwitch?.removeCallbacks(mStateRunnable)
    }

    // ---- Listeners ----

    override fun onPreferenceClick(preference: Preference): Boolean {
        return when (preference) {
            mPrefAdd -> {
                addProfile()
                true
            }
            mPrefDel -> {
                removeProfile()
                true
            }
            mPrefAppSelector -> {
                showAppSelector()
                true
            }
            else -> false
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference) {
            mPrefProfile -> {
                val name = newValue.toString()
                mProfile = mManager.getProfile(name)
                mManager.switchDefault(name)
                reload()
                true
            }
            mPrefServer -> {
                mProfile!!.server = newValue.toString()
                resetTextN(mPrefServer, newValue)
                true
            }
            mPrefPort -> {
                if (TextUtils.isEmpty(newValue.toString())) return false
                mProfile!!.port = Integer.parseInt(newValue.toString())
                resetTextN(mPrefPort, newValue)
                true
            }
            mPrefUserpw -> {
                mProfile!!.isUserPw = java.lang.Boolean.parseBoolean(newValue.toString())
                true
            }
            mPrefUsername -> {
                mProfile!!.username = newValue.toString()
                resetTextN(mPrefUsername, newValue)
                true
            }
            mPrefPassword -> {
                mProfile!!.password = newValue.toString()
                resetTextN(mPrefPassword, newValue)
                true
            }
            mPrefRoutes -> {
                mProfile!!.route = newValue.toString()
                resetListN(mPrefRoutes, newValue)
                true
            }
            mPrefDnsPresets -> {
                val preset = newValue.toString()
                if (!TextUtils.isEmpty(preset)) {
                    mProfile!!.dns = preset
                    resetTextN(mPrefDns, preset)
                    mPrefDns.text = preset
                }
                true
            }
            mPrefDns -> {
                mProfile!!.dns = newValue.toString()
                resetTextN(mPrefDns, newValue)
                true
            }
            mPrefDnsPort -> {
                if (TextUtils.isEmpty(newValue.toString())) return false
                mProfile!!.dnsPort = Integer.valueOf(newValue.toString())
                resetTextN(mPrefDnsPort, newValue)
                true
            }
            mPrefPerApp -> {
                mProfile!!.isPerApp = java.lang.Boolean.parseBoolean(newValue.toString())
                true
            }
            mPrefAppBypass -> {
                mProfile!!.isBypassApp = java.lang.Boolean.parseBoolean(newValue.toString())
                true
            }
            mPrefAppList -> {
                mProfile!!.appList = newValue.toString()
                true
            }
            mPrefIPv6 -> {
                mProfile!!.hasIPv6 = java.lang.Boolean.parseBoolean(newValue.toString())
                true
            }
            mPrefUDP -> {
                mProfile!!.hasUDP = java.lang.Boolean.parseBoolean(newValue.toString())
                true
            }
            mPrefUDPGW -> {
                mProfile!!.udpgw = newValue.toString()
                resetTextN(mPrefUDPGW, newValue)
                true
            }
            mPrefAuto -> {
                mProfile!!.autoConnect = java.lang.Boolean.parseBoolean(newValue.toString())
                true
            }
            mPrefDynamic -> {
                Toast.makeText(requireActivity(), "Restart app to apply theme changes", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            startVpn()
        } else {
            stopVpn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            Utility.startVpn(requireActivity(), mProfile!!)
            checkState()
        }
    }

    // ---- Preference wiring ----

    private fun initPreferences() {
        mPrefProfile = findPreference(PREF_PROFILE) as ListPreference
        mPrefServer = findPreference(PREF_SERVER_IP) as EditTextPreference
        mPrefPort = findPreference(PREF_SERVER_PORT) as EditTextPreference
        mPrefUserpw = findPreference(PREF_AUTH_USERPW) as CheckBoxPreference
        mPrefUsername = findPreference(PREF_AUTH_USERNAME) as EditTextPreference
        mPrefPassword = findPreference(PREF_AUTH_PASSWORD) as EditTextPreference
        mPrefRoutes = findPreference(PREF_ADV_ROUTE) as ListPreference
        mPrefDnsPresets = findPreference("adv_dns_presets") as ListPreference
        mPrefDns = findPreference(PREF_ADV_DNS) as EditTextPreference
        mPrefDnsPort = findPreference(PREF_ADV_DNS_PORT) as EditTextPreference
        mPrefPerApp = findPreference(PREF_ADV_PER_APP) as CheckBoxPreference
        mPrefAppBypass = findPreference(PREF_ADV_APP_BYPASS) as CheckBoxPreference
        mPrefAppList = findPreference(PREF_ADV_APP_LIST) as EditTextPreference
        mPrefIPv6 = findPreference(PREF_IPV6_PROXY) as CheckBoxPreference
        mPrefUDP = findPreference(PREF_UDP_PROXY) as CheckBoxPreference
        mPrefUDPGW = findPreference(PREF_UDP_GW) as EditTextPreference
        mPrefAuto = findPreference(PREF_ADV_AUTO_CONNECT) as CheckBoxPreference
        mPrefDynamic = findPreference(PREF_DYNAMIC_COLORS) as? CheckBoxPreference
        mPrefAdd = findPreference("prof_add_btn")!!
        mPrefDel = findPreference("prof_del_btn")!!
        mPrefAppSelector = findPreference("adv_app_selector")!!

        mPrefProfile.onPreferenceChangeListener = this
        mPrefServer.onPreferenceChangeListener = this
        mPrefPort.onPreferenceChangeListener = this
        mPrefUserpw.onPreferenceChangeListener = this
        mPrefUsername.onPreferenceChangeListener = this
        mPrefPassword.onPreferenceChangeListener = this
        mPrefRoutes.onPreferenceChangeListener = this
        mPrefDnsPresets.onPreferenceChangeListener = this
        mPrefDns.onPreferenceChangeListener = this
        mPrefDnsPort.onPreferenceChangeListener = this
        mPrefPerApp.onPreferenceChangeListener = this
        mPrefAppBypass.onPreferenceChangeListener = this
        mPrefAppList.onPreferenceChangeListener = this
        mPrefIPv6.onPreferenceChangeListener = this
        mPrefUDP.onPreferenceChangeListener = this
        mPrefUDPGW.onPreferenceChangeListener = this
        mPrefAuto.onPreferenceChangeListener = this
        mPrefDynamic?.onPreferenceChangeListener = this
        mPrefAdd.onPreferenceClickListener = this
        mPrefDel.onPreferenceClickListener = this
        mPrefAppSelector.onPreferenceClickListener = this
    }

    // ---- State helpers ----

    private fun reload() {
        if (mProfile == null) {
            mProfile = mManager.default
        }

        mPrefProfile.entries = mManager.profiles
        mPrefProfile.entryValues = mManager.profiles
        mPrefProfile.value = mProfile!!.name
        mPrefRoutes.value = mProfile!!.route
        resetList(mPrefProfile, mPrefRoutes)

        mPrefUserpw.isChecked = mProfile!!.isUserPw
        mPrefPerApp.isChecked = mProfile!!.isPerApp
        mPrefAppBypass.isChecked = mProfile!!.isBypassApp
        mPrefIPv6.isChecked = mProfile!!.hasIPv6
        mPrefUDP.isChecked = mProfile!!.hasUDP
        mPrefAuto.isChecked = mProfile!!.autoConnect

        mPrefDynamic?.isChecked = PreferenceManager.getDefaultSharedPreferences(requireActivity())
            .getBoolean(PREF_DYNAMIC_COLORS, true)

        mPrefServer.text = mProfile!!.server
        mPrefPort.text = mProfile!!.port.toString()
        mPrefUsername.text = mProfile!!.username
        mPrefPassword.text = mProfile!!.password
        mPrefDns.text = mProfile!!.dns
        mPrefDnsPort.text = mProfile!!.dnsPort.toString()
        mPrefUDPGW.text = mProfile!!.udpgw
        resetText(mPrefServer, mPrefPort, mPrefUsername, mPrefPassword, mPrefDns, mPrefDnsPort, mPrefUDPGW)

        mPrefAppList.text = mProfile!!.appList
    }

    private fun resetList(vararg pref: ListPreference) {
        for (p in pref) {
            p.summary = p.entry
        }
    }

    private fun resetListN(pref: ListPreference, newValue: Any?) {
        pref.summary = newValue.toString()
    }

    private fun resetText(vararg pref: EditTextPreference) {
        for (p in pref) {
            if ((p.editText.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                p.summary = p.text
            } else {
                p.summary = if (p.text.isNotEmpty()) {
                    "*".repeat(p.text.length)
                } else {
                    ""
                }
            }
        }
    }

    private fun resetTextN(pref: EditTextPreference, newValue: Any?) {
        if ((pref.editText.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            pref.summary = newValue.toString()
        } else {
            val text = newValue.toString()
            pref.summary = if (text.isNotEmpty()) {
                "*".repeat(text.length)
            } else {
                ""
            }
        }
    }

    // ---- Profile management ----

    private fun addProfile() {
        val e = EditText(requireActivity())
        e.isSingleLine = true

        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.prof_add)
            .setView(e)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = e.text.toString().trim()
                val p = if (!TextUtils.isEmpty(name)) mManager.addProfile(name) else null
                if (p != null) {
                    mProfile = p
                    reload()
                } else {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.err_add_prof, name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    private fun removeProfile() {
        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.prof_del)
            .setMessage(getString(R.string.prof_del_confirm, mProfile!!.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (!mManager.removeProfile(mProfile!!.name)) {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.err_del_prof, mProfile!!.name),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mProfile = mManager.default
                    reload()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    // ---- VPN state management ----

    private fun checkState() {
        mRunning = false
        mSwitch?.isEnabled = false
        mSwitch?.setOnCheckedChangeListener(null)

        if (mBinder == null) {
            requireActivity().bindService(
                Intent(requireActivity(), SocksVpnService::class.java),
                mConnection,
                0
            )
        }
    }

    private fun showAppSelector() {
        AppSelector.show(requireActivity(), mProfile!!.appList) { appList ->
            mProfile!!.appList = appList
            mPrefAppList.text = appList
        }
    }

    private fun updateState() {
        if (mSwitch == null) return

        mRunning = if (mBinder == null) {
            false
        } else {
            try {
                mBinder!!.isRunning
            } catch (e: Exception) {
                false
            }
        }

        mSwitch?.isChecked = mRunning

        if ((!mStarting && !mStopping) || (mStarting && mRunning) || (mStopping && !mRunning)) {
            mSwitch?.isEnabled = true
        }

        if (mStarting && mRunning) {
            mStarting = false
        }

        if (mStopping && !mRunning) {
            mStopping = false
        }

        mSwitch?.setOnCheckedChangeListener(this)
    }

    private fun startVpn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireActivity().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requireActivity().requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
                return
            }
        }
        mStarting = true
        val i = VpnService.prepare(requireActivity())

        if (i != null) {
            vpnPrepareLauncher.launch(i)
        } else {
            onActivityResult(0, Activity.RESULT_OK, null)
        }
    }

    private fun stopVpn() {
        if (mBinder == null) return

        mStopping = true

        try {
            mBinder!!.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop VPN", e)
        }

        mBinder = null

        requireActivity().unbindService(mConnection)
        checkState()
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}
