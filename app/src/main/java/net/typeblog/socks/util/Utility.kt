package net.typeblog.socks.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import net.typeblog.socks.BuildConfig.DEBUG
import net.typeblog.socks.SocksVpnService
import net.typeblog.socks.util.Constants.INTENT_APP_BYPASS
import net.typeblog.socks.util.Constants.INTENT_APP_LIST
import net.typeblog.socks.util.Constants.INTENT_DNS
import net.typeblog.socks.util.Constants.INTENT_DNS_PORT
import net.typeblog.socks.util.Constants.INTENT_IPV6_PROXY
import net.typeblog.socks.util.Constants.INTENT_NAME
import net.typeblog.socks.util.Constants.INTENT_PER_APP
import net.typeblog.socks.util.Constants.INTENT_PORT
import net.typeblog.socks.util.Constants.INTENT_ROUTE
import net.typeblog.socks.util.Constants.INTENT_SERVER
import net.typeblog.socks.util.Constants.INTENT_USERNAME
import net.typeblog.socks.util.Constants.INTENT_PASSWORD
import net.typeblog.socks.util.Constants.INTENT_UDP_GW
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class IpInfo(val ip: String, val countryCode: String)

object Utility {
    private val TAG = Utility::class.java.simpleName

    @JvmStatic
    fun extractFile(context: Context) {
        // No longer needed: we run libpdnsd.so and libtun2socks.so directly from nativeLibraryDir
    }

    @JvmStatic
    fun exec(cmd: String): Int {
        return try {
            Log.d(TAG, "Executing: $cmd")
            val p = Runtime.getRuntime().exec(cmd)

            val br = java.io.BufferedReader(java.io.InputStreamReader(p.errorStream))
            var line = br.readLine()
            while (line != null) {
                Log.e(TAG, "STDERR: $line")
                line = br.readLine()
            }

            val ret = p.waitFor()
            Log.d(TAG, "Process exited with: $ret")
            ret
        } catch (e: Exception) {
            Log.e(TAG, "exec failed", e)
            -1
        }
    }

    @JvmStatic
    fun exec(cmd: Array<String>): Int {
        return try {
            Log.d(TAG, "Executing: ${cmd.contentToString()}")
            val p = Runtime.getRuntime().exec(cmd)

            val br = java.io.BufferedReader(java.io.InputStreamReader(p.errorStream))
            var line = br.readLine()
            while (line != null) {
                Log.e(TAG, "STDERR: $line")
                line = br.readLine()
            }

            val ret = p.waitFor()
            Log.d(TAG, "Process exited with: $ret")
            ret
        } catch (e: Exception) {
            Log.e(TAG, "exec failed", e)
            -1
        }
    }

    @JvmStatic
    fun killPidFile(f: String) {
        val file = File(f)

        if (!file.exists()) {
            return
        }

        val i: InputStream = try {
            FileInputStream(file)
        } catch (e: Exception) {
            return
        }

        val buf = ByteArray(512)
        val str = StringBuilder()

        try {
            var len = i.read(buf, 0, 512)
            while (len > 0) {
                str.append(String(buf, 0, len))
                len = i.read(buf, 0, 512)
            }
            i.close()
        } catch (e: Exception) {
            return
        }

        try {
            val pid = str.toString().trim().replace("\n", "").toInt()
            Runtime.getRuntime().exec("kill $pid").waitFor()
            file.delete()
        } catch (e: Exception) {
            // ignore
        }
    }

    @JvmStatic
    fun join(list: List<String>?, separator: String): String {
        if (list == null || list.isEmpty()) return ""
        val ret = StringBuilder()

        for (s in list) {
            ret.append(s).append(separator)
        }

        return ret.substring(0, ret.length - separator.length)
    }

    @JvmStatic
    fun makePdnsdConf(context: Context, dns: String, port: Int) {
        val dir = context.filesDir.absolutePath
        val conf = String.format(context.getString(net.typeblog.socks.R.string.pdnsd_conf), dir, dir, dns, port)

        val f = File("$dir/pdnsd.conf")

        if (f.exists()) {
            f.delete()
        }

        try {
            val out = FileOutputStream(f)
            out.write(conf.toByteArray())
            out.flush()
            out.close()
        } catch (e: Exception) {
            // ignore
        }

        val cache = File("$dir/pdnsd.cache")

        if (!cache.exists()) {
            try {
                cache.createNewFile()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    @JvmStatic
    fun startVpn(context: Context, profile: Profile) {
        val i = Intent(context, SocksVpnService::class.java)
            .putExtra(INTENT_NAME, profile.getName())
            .putExtra(INTENT_SERVER, profile.getServer())
            .putExtra(INTENT_PORT, profile.getPort())
            .putExtra(INTENT_ROUTE, profile.getRoute())
            .putExtra(INTENT_DNS, profile.getDns())
            .putExtra(INTENT_DNS_PORT, profile.getDnsPort())
            .putExtra(INTENT_PER_APP, profile.isPerApp())
            .putExtra(INTENT_IPV6_PROXY, profile.hasIPv6())

        if (profile.isPerApp()) {
            i.putExtra(INTENT_APP_BYPASS, profile.isBypassApp())
                .putExtra(INTENT_APP_LIST, profile.getAppList().split("\n").toTypedArray())
        }

        i.putExtra(INTENT_USERNAME, profile.getUsername())
        i.putExtra(INTENT_PASSWORD, profile.getPassword())

        if (profile.hasUDP()) {
            i.putExtra(INTENT_UDP_GW, profile.getUDPGW())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i)
        } else {
            context.startService(i)
        }
    }

    @JvmStatic
    fun checkPublicIp(): IpInfo? {
        return try {
            val url = URL("http://ip-api.com/line/?fields=query,countryCode")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val line = reader.readLine()
            reader.close()
            conn.disconnect()
            if (line != null && line.contains(",")) {
                val parts = line.split(",")
                IpInfo(parts[0], parts[1])
            } else null
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun countryCodeToFlag(countryCode: String): String {
        if (countryCode.length != 2) return "\uD83C\uDF10"
        val firstChar = Character.toChars(0x1F1E6 - 'A'.code + countryCode[0].uppercaseChar().code)[0]
        val secondChar = Character.toChars(0x1F1E6 - 'A'.code + countryCode[1].uppercaseChar().code)[0]
        return "$firstChar$secondChar"
    }
}
