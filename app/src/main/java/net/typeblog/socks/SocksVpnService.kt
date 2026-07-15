package net.typeblog.socks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
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
import net.typeblog.socks.util.Constants.ACTION_STOP_VPN
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
import net.typeblog.socks.util.Constants.PREF_NOTIFICATION_CONTROL
import net.typeblog.socks.util.Routes
import net.typeblog.socks.util.Utility
import net.typeblog.socks.BuildConfig.DEBUG

class SocksVpnService : VpnService() {
    inner class VpnBinder : IVpnService.Stub() {
        override fun isRunning(): Boolean {
            return mRunning
        }

        override fun stop() {
            Log.d(TAG, "stop() called via AIDL binder")
            stopMe("binder_stop")
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
    private var mProfileName: String? = null
    private var mTun2socksProcess: java.lang.Process? = null

    private var mCurrentIp: String? = null
    private var mCountryCode: String? = null
    private var mConnectedSince: Long = 0L
    private val mIpCheckHandler = Handler(Looper.getMainLooper())
    private val mScreenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off received, auto-stopping VPN")
                stopMe("screen_off")
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
                        updateNotification()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "IP check failed", e)
                }
            }.start()
            mIpCheckHandler.postDelayed(this, 30000)
        }
    }

    private val mNotificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_STOP_VPN -> {
                    Log.d(TAG, "Notification stop action received")
                    stopMe("notification_stop")
                }
            }
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

        mProfileName = intent.getStringExtra(INTENT_NAME)
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

        Log.d(TAG, "onStartCommand: profile=$mProfileName server=$server:$port user=$username route=$route dns=$dns:$dnsPort perApp=$perApp ipv6=$ipv6 udpgw=$udpgw")

        createNotificationChannel()

        showNotification()

            // Register notification action receiver
        registerReceiver(mNotificationActionReceiver, IntentFilter(ACTION_STOP_VPN), null, null)

        configure(mProfileName, route, perApp, appBypass, appList, ipv6)

        if (DEBUG)
            Log.d(TAG, "fd: ${mInterface?.fd}")

        if (mInterface != null) {
            Log.d(TAG, "mInterface is non-null with fd=${mInterface!!.fd}, calling start()")
            start(mInterface!!.fd, server, port, username, passwd, dns, dnsPort, ipv6, udpgw)
        } else {
            Log.e(TAG, "mInterface is NULL after configure() — VPN establish() returned null!")
            stopMe("interface_null")
        }

        if (mRunning) {
            mConnectedSince = java.lang.System.currentTimeMillis()
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
        Log.d(TAG, "onRevoke called - VPN permission revoked")
        super.onRevoke()
        stopMe("vpn_revoked")
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
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
        stopMe("on_destroy")
    }

    private fun stopMe(reason: String = "") {
        Log.d(TAG, "stopMe called" + if (reason.isNotEmpty()) " - reason: $reason" else "")
        if (reason.isEmpty()) {
            // Log stack trace when no reason is given to identify caller
            Log.d(TAG, "stopMe stack trace:", Throwable("stopMe caller trace"))
        }
        stopForeground(true)

        val dir = filesDir.absolutePath

        // Kill tun2socks: destroy Process handle or fall back to pid file
        mTun2socksProcess?.let { p ->
            try {
                p.destroy()
                Log.d(TAG, "tun2socks process destroyed")
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying tun2socks process: ${e.message}")
            }
            mTun2socksProcess = null
        }
        Utility.killPidFile("$dir/tun2socks.pid")
        Utility.killPidFile("$dir/pdnsd.pid")

        try {
            mInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
        }

        mProfileName = null
        mCurrentIp = null
        mCountryCode = null
        mConnectedSince = 0L
        mRunning = false

        mIpCheckHandler.removeCallbacks(mIpCheckRunnable)

        try {
            unregisterReceiver(mNotificationActionReceiver)
        } catch (_: Exception) { }

        try {
            unregisterReceiver(mScreenOffReceiver)
        } catch (e: Exception) { }

        stopSelf()
    }

    private fun showNotification() {
        val stopIntent = Intent(ACTION_STOP_VPN).apply {
            setPackage(packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "vpn_service")
        } else {
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle(getString(R.string.notify_title))
            .setContentText(getString(R.string.notify_msg, mProfileName ?: ""))
            .setSmallIcon(R.drawable.ic_launcher)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun updateNotification() {
        if (!mRunning) return

        val stopIntent = Intent(ACTION_STOP_VPN).apply { setPackage(packageName) }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "vpn_service")
        } else {
            Notification.Builder(this)
        }

        val ipText = if (!mCurrentIp.isNullOrEmpty()) {
            "IP: $mCurrentIp"
        } else {
            "Connecting..."
        }

        val notification = builder
            .setContentTitle(getString(R.string.notify_title))
            .setContentText(ipText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, notification)
    }

    private fun configure(name: String?, route: String?, perApp: Boolean, bypass: Boolean, apps: Array<String>?, ipv6: Boolean) {
        val b = Builder()
        b.setMtu(1500)
            .setSession(name ?: "KiloProxy")
            .addAddress("10.10.10.1", 24)
            .addDnsServer("8.8.8.8")

        if (ipv6) {
            b.addAddress("fdfe:dcba:9876::1", 126)
                .addRoute("::", 0)
        }

        Routes.addRoutes(this, b, route ?: "all")

        b.addRoute("8.8.8.8", 32)

        if (!perApp) {
            try {
                b.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
            }
        } else {
            if (bypass) {
                try {
                    b.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}", e)
                }
                for (p in apps!!) {
                    if (TextUtils.isEmpty(p)) continue
                    try {
                        b.addDisallowedApplication(p.trim { it <= ' ' })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                }
            } else {
                for (p in apps!!) {
                    if (TextUtils.isEmpty(p) || p.trim { it <= ' ' } == packageName) continue
                    try {
                        b.addAllowedApplication(p.trim { it <= ' ' })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                }
            }
        }

        mInterface = b.establish()
        if (mInterface == null) {
            Log.e(TAG, "VpnService.Builder.establish() returned null")
        } else {
            Log.d(TAG, "VpnService established with fd=${mInterface!!.fd}")
        }
    }

    private fun start(fd: Int, server: String?, port: Int, user: String?, passwd: String?, dns: String?, dnsPort: Int, ipv6: Boolean, udpgw: String?) {
        Utility.makePdnsdConf(this, dns ?: "8.8.8.8", dnsPort)

        val libDir = applicationInfo.nativeLibraryDir
        val dir = filesDir.absolutePath

        val pdnsdResult = Utility.exec(arrayOf(
            "$libDir/libpdnsd.so",
            "-c",
            "$dir/pdnsd.conf"
        ))
        Log.d(TAG, "pdnsd exec returned: $pdnsdResult")

        val command = mutableListOf(
            "$libDir/libtun2socks.so",
            "--netif-ipaddr", "10.10.10.2",
            "--netif-netmask", "255.255.255.0",
            "--socks-server-addr", "$server:$port",
            "--tunfd", fd.toString(),
            "--tunmtu", "1500",
            "--loglevel", "3"
        )

        if (!user.isNullOrEmpty()) {
            command.add("--username")
            command.add(user!!)
            command.add("--password")
            command.add(passwd ?: "")
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

        Log.d(TAG, "tun2socks full command: ${command.joinToString(" ")}")
        
        // Start tun2socks non-blocking (no daemonization). Store Process for later cleanup.
        try {
            val pb = ProcessBuilder(command)
            pb.redirectErrorStream(true)
            val process = pb.start()
            mTun2socksProcess = process
            Log.d(TAG, "tun2socks process started with PID awareness")
            
            // Consume stdout/stderr on a background thread to prevent buffer deadlock
            Thread {
                try {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                    var line = reader.readLine()
                    while (line != null) {
                        Log.d(TAG, "tun2socks: $line")
                        line = reader.readLine()
                    }
                    val exitCode = process.waitFor()
                    Log.d(TAG, "tun2socks process exited with: $exitCode")
                    if (exitCode != 0 && mRunning) {
                        Log.e(TAG, "tun2socks exited unexpectedly with code $exitCode")
                        // Only stop if we haven't already initiated shutdown
                        runOnMainThread { stopMe("tun2socks_exited:$exitCode") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "tun2socks monitor error: ${e.message}")
                }
            }.apply { isDaemon = true }.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tun2socks process", e)
            stopMe("tun2socks_start_failed:${e.message}")
            return
        }

        var i = 0
        while (i < 5) {
            val sendResult = System.sendfd(fd)
            Log.d(TAG, "sendfd attempt ${i + 1}/5 returned: $sendResult")
            if (sendResult != -1) {
                Log.d(TAG, "sendfd succeeded on attempt ${i + 1}")
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

        Log.e(TAG, "sendfd failed after 5 attempts, stopping VPN")
        stopMe("sendfd_failed_5_attempts")
    }

    private fun runOnMainThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }

    companion object {
        private const val TAG = "SocksVpnService"
    }
}
