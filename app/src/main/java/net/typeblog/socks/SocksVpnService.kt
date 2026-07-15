package net.typeblog.socks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.net.VpnService.Builder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.text.TextUtils
import android.util.Log
import androidx.preference.PreferenceManager
import net.typeblog.socks.R
import net.typeblog.socks.util.Constants
import net.typeblog.socks.util.Routes
import net.typeblog.socks.util.Utility
import net.typeblog.socks.util.Constants.INTENT_APP_BYPASS
import net.typeblog.socks.util.Constants.INTENT_APP_LIST
import net.typeblog.socks.util.Constants.INTENT_DNS
import net.typeblog.socks.util.Constants.INTENT_DNS_PORT
import net.typeblog.socks.util.Constants.INTENT_IPV6_PROXY
import net.typeblog.socks.util.Constants.INTENT_NAME
import net.typeblog.socks.util.Constants.INTENT_PASSWORD
import net.typeblog.socks.util.Constants.INTENT_PER_APP
import net.typeblog.socks.util.Constants.INTENT_PORT
import net.typeblog.socks.util.Constants.INTENT_ROUTE
import net.typeblog.socks.util.Constants.INTENT_SERVER
import net.typeblog.socks.util.Constants.INTENT_UDP_GW
import net.typeblog.socks.util.Constants.INTENT_USERNAME
import net.typeblog.socks.util.Constants.PREF_AUTO_STOP
import net.typeblog.socks.BuildConfig.DEBUG

class SocksVpnService : VpnService() {
    inner class VpnBinder : IVpnService.Stub() {
        override fun isRunning(): Boolean {
            return mRunning
        }

        override fun stop() {
            stopMe()
        }

        override fun getCurrentIp(): String {
            return mCurrentIp ?: ""
        }

        override fun getCountryCode(): String {
            return mCountryCode ?: ""
        }

        override fun getConnectedSince(): Long {
            return mConnectedSince
        }
    }

    private var mInterface: ParcelFileDescriptor? = null
    private var mRunning = false
    private val mBinder: IBinder = VpnBinder()

    private var mCurrentIp: String? = null
    private var mCountryCode: String? = null
    private var mConnectedSince: Long = 0L
    private val mIpCheckHandler = Handler(Looper.getMainLooper())
    private val mScreenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off received, auto-stopping VPN")
                stopMe()
            }
        }
    }
    private val mIpCheckRunnable = object : Runnable {
        override fun run() {
            Thread {
                try {
                    val info = Utility.checkPublicIp()
                    if (info != null) {
                        mCurrentIp = info.ip
                        mCountryCode = info.countryCode
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "IP check failed", e)
                }
            }.start()
            mIpCheckHandler.postDelayed(this, 30000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "vpn_service",
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            if (manager != null) {
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) {
            Log.d(TAG, "starting")
        }

        if (intent == null) {
            return 0
        }

        if (mRunning) {
            return 0
        }

        val name = intent.getStringExtra(INTENT_NAME)
        val server = intent.getStringExtra(INTENT_SERVER)
        val port = intent.getIntExtra(INTENT_PORT, 1080)
        val username = intent.getStringExtra(INTENT_USERNAME)
        val passwd = intent.getStringExtra(INTENT_PASSWORD)
        val route = intent.getStringExtra(INTENT_ROUTE)
        val dns = intent.getStringExtra(INTENT_DNS)
        val dnsPort = intent.getIntExtra(INTENT_DNS_PORT, 53)
        val perApp = intent.getBooleanExtra(INTENT_PER_APP, false)
        val appBypass = intent.getBooleanExtra(INTENT_APP_BYPASS, false)
        val appList = intent.getStringArrayExtra(INTENT_APP_LIST)
        val ipv6 = intent.getBooleanExtra(INTENT_IPV6_PROXY, false)
        val udpgw = intent.getStringExtra(INTENT_UDP_GW)

        // Create the notification channel
        createNotificationChannel()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "vpn_service")
        } else {
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle(getString(R.string.notify_title))
            .setContentText(String.format(getString(R.string.notify_msg), name))
            .setPriority(Notification.PRIORITY_MIN)
            .setSmallIcon(R.drawable.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        // Create an fd.
        configure(name, route, perApp, appBypass, appList, ipv6)

        if (DEBUG)
            Log.d(TAG, "fd: ${mInterface?.fd}")

        if (mInterface != null)
            start(mInterface!!.fd, server, port, username, passwd, dns, dnsPort, ipv6, udpgw)

        if (mRunning) {
            mConnectedSince = System.currentTimeMillis()
            mIpCheckHandler.post(mIpCheckRunnable)

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            if (prefs.getBoolean(PREF_AUTO_STOP, false)) {
                val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
                registerReceiver(mScreenOffReceiver, filter)
            }
        }

        return START_STICKY
    }

    override fun onRevoke() {
        super.onRevoke()
        stopMe()
    }

    override fun onBind(intent: Intent?): IBinder? {
        val callingUid = android.os.Binder.getCallingUid()
        if (callingUid != Process.SYSTEM_UID && callingUid != Process.myUid()) {
            Log.w(TAG, "Unauthorized bind attempt from UID $callingUid")
            return null
        }
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMe()
    }

    private fun stopMe() {
        stopForeground(true)

        val dir = filesDir.absolutePath

        Utility.killPidFile("$dir/tun2socks.pid")
        Utility.killPidFile("$dir/pdnsd.pid")

        try {
            mInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
        }

        mCurrentIp = null
        mCountryCode = null
        mConnectedSince = 0L
        mRunning = false

        mIpCheckHandler.removeCallbacks(mIpCheckRunnable)

        try {
            unregisterReceiver(mScreenOffReceiver)
        } catch (e: Exception) {
            // Receiver may not have been registered
        }

        stopSelf()
    }

    private fun configure(name: String?, route: String?, perApp: Boolean, bypass: Boolean, apps: Array<String>?, ipv6: Boolean) {
        val b = Builder()
        b.setMtu(1500)
            .setSession(name ?: "SocksDroid")
            .addAddress("10.10.10.1", 24)
            .addDnsServer("8.8.8.8")

        if (ipv6) {
            // Route all IPv6 traffic
            b.addAddress("fdfe:dcba:9876::1", 126)
                .addRoute("::", 0)
        }

        Routes.addRoutes(this, b, route ?: "all")

        // Add the default DNS
        // Note that this DNS is just a stub.
        // Actual DNS requests will be redirected through pdnsd.
        b.addRoute("8.8.8.8", 32)

        // Do app routing
        if (!perApp) {
            // Just bypass myself
            try {
                b.addDisallowedApplication("net.typeblog.socks")
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
            }
        } else {
            if (bypass) {
                // First, bypass myself
                try {
                    b.addDisallowedApplication("net.typeblog.socks")
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}", e)
                }

                for (p in apps!!) {
                    if (TextUtils.isEmpty(p))
                        continue

                    try {
                        b.addDisallowedApplication(p.trim { it <= ' ' })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                }
            } else {
                for (p in apps!!) {
                    if (TextUtils.isEmpty(p) || p.trim { it <= ' ' } == "net.typeblog.socks") {
                        continue
                    }

                    try {
                        b.addAllowedApplication(p.trim { it <= ' ' })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                }
            }
        }

        mInterface = b.establish()
    }

    private fun start(fd: Int, server: String?, port: Int, user: String?, passwd: String?, dns: String?, dnsPort: Int, ipv6: Boolean, udpgw: String?) {
        // Start DNS daemon first
        Utility.makePdnsdConf(this, dns ?: "8.8.8.8", dnsPort)

        val libDir = applicationInfo.nativeLibraryDir
        val dir = filesDir.absolutePath

        Utility.exec(arrayOf(
            "$libDir/libpdnsd.so",
            "-c",
            "$dir/pdnsd.conf"
        ))

        val command = mutableListOf(
            "$libDir/libtun2socks.so",
            "--netif-ipaddr", "10.10.10.2",
            "--netif-netmask", "255.255.255.0",
            "--socks-server-addr", "$server:$port",
            "--tunfd", fd.toString(),
            "--tunmtu", "1500",
            "--loglevel", "3",
            "--pid", "$dir/tun2socks.pid"
        )

        if (!user.isNullOrEmpty()) {
            command.add("--username")
            command.add(user!!)
        }
        if (!passwd.isNullOrEmpty()) {
            command.add("--password")
            command.add(passwd!!)
        }

        if (ipv6) {
            command.add("--netif-ip6addr")
            command.add("fdfe:dcba:9876::2")
        }

        command.add("--dnsgw")
        command.add("10.10.10.1:8091")

        if (udpgw != null && udpgw.isNotEmpty()) {
            command.add("--udpgw-remote-server-addr")
            command.add(udpgw)
        }

        if (DEBUG) {
            Log.d(TAG, command.toString())
        }

        if (Utility.exec(command.toTypedArray()) != 0) {
            stopMe()
            return
        }

        // Try to send the Fd through socket.
        var i = 0
        while (i < 5) {
            if (System.sendfd(fd) != -1) {
                mRunning = true
                return
            }

            i++

            try {
                Thread.sleep((1000 * i).toLong())
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
            }
        }

        // Should not get here. Must be a failure.
        stopMe()
    }

    companion object {
        private const val TAG = "SocksVpnService"
    }
}
