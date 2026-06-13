package net.typeblog.socks.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.List;

import net.typeblog.socks.R;
import net.typeblog.socks.SocksVpnService;
import net.typeblog.socks.System;
import static net.typeblog.socks.BuildConfig.DEBUG;
import static net.typeblog.socks.util.Constants.*;

public class Utility {
	private static final String TAG = Utility.class.getSimpleName();
	
	public static void extractFile(Context context) {
		// No longer needed: we run libpdnsd.so and libtun2socks.so directly from nativeLibraryDir
	}
	
	public static int exec(String cmd) {
		try {
			Log.d(TAG, "Executing: " + cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			
			// Read stderr before waitFor to prevent deadlocks if stderr buffer fills up
			java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null) {
				Log.e(TAG, "STDERR: " + line);
			}
			
			int ret = p.waitFor();
			Log.d(TAG, "Process exited with: " + ret);
			return ret;
		} catch (Exception e) {
			Log.e(TAG, "exec failed", e);
			return -1;
		}
	}
	
	public static int exec(String[] cmd) {
		try {
			Log.d(TAG, "Executing: " + java.util.Arrays.toString(cmd));
			Process p = Runtime.getRuntime().exec(cmd);
			
			java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(p.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null) {
				Log.e(TAG, "STDERR: " + line);
			}
			
			int ret = p.waitFor();
			Log.d(TAG, "Process exited with: " + ret);
			return ret;
		} catch (Exception e) {
			Log.e(TAG, "exec failed", e);
			return -1;
		}
	}
	
	public static void killPidFile(String f) {
		File file = new File(f);
		
		if (!file.exists()) {
			return;
		}
		
		InputStream i = null;
		try {
			i = new FileInputStream(file);
		} catch (Exception e) {
			return;
		}
		
		byte[] buf = new byte[512];
		int len;
		StringBuilder str = new StringBuilder();
		
		try {
			while ((len = i.read(buf, 0, 512)) > 0) {
				str.append(new String(buf, 0, len));
			}
			i.close();
		} catch (Exception e) {
			return;
		}
		
		try {
			int pid = Integer.parseInt(str.toString().trim().replace("\n", ""));
			Runtime.getRuntime().exec("kill " + pid).waitFor();
			file.delete();
		} catch (Exception e) {
			
		}
	}
	
	public static String join(List<String> list, String separator) {
		if (list == null || list.isEmpty()) return "";
		StringBuilder ret = new StringBuilder();
		
		for (String s : list) {
			ret.append(s).append(separator);
		}
		
		return ret.substring(0, ret.length() - separator.length());
	}
	
	public static void makePdnsdConf(Context context, String dns, int port) {
		String dir = context.getFilesDir().getAbsolutePath();
		String conf = String.format(context.getString(R.string.pdnsd_conf), dir, dir, dns, port);
		
		File f = new File(dir + "/pdnsd.conf");
		
		if (f.exists()) {
			f.delete();
		}
		
		try {
			OutputStream out = new FileOutputStream(f);
			out.write(conf.getBytes());
			out.flush();
			out.close();
		} catch (Exception e) {
			
		}
		
		File cache = new File(dir + "/pdnsd.cache");
		
		if (!cache.exists()) {
			try {
				cache.createNewFile();
			} catch (Exception e) {
				
			}
		}
	}
	
	public static void startVpn(Context context, Profile profile) {
		Intent i = new Intent(context, SocksVpnService.class)
			.putExtra(INTENT_NAME, profile.getName())
			.putExtra(INTENT_SERVER, profile.getServer())
			.putExtra(INTENT_PORT, profile.getPort())
			.putExtra(INTENT_ROUTE, profile.getRoute())
			.putExtra(INTENT_DNS, profile.getDns())
			.putExtra(INTENT_DNS_PORT, profile.getDnsPort())
			.putExtra(INTENT_PER_APP, profile.isPerApp())
			.putExtra(INTENT_IPV6_PROXY, profile.hasIPv6());

		if (profile.isUserPw()) {
			i.putExtra(INTENT_USERNAME, profile.getUsername())
				.putExtra(INTENT_PASSWORD, profile.getPassword());
		}

		if (profile.isPerApp()) {
			i.putExtra(INTENT_APP_BYPASS, profile.isBypassApp())
				.putExtra(INTENT_APP_LIST, profile.getAppList().split("\n"));
		}

		if (profile.hasUDP()) {
			i.putExtra(INTENT_UDP_GW, profile.getUDPGW());
		}
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(i);
		} else {
			context.startService(i);
		}
	}
}
