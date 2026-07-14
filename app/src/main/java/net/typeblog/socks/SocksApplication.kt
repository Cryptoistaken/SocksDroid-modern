package net.typeblog.socks

import android.app.Application
import android.preference.PreferenceManager

import com.google.android.material.color.DynamicColors

import net.typeblog.socks.util.Constants.PREF_DYNAMIC_COLORS

class SocksApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Ensure default preference values are set before reading
        PreferenceManager.setDefaultValues(this, R.xml.settings, true)

        // Apply dynamic colors if enabled in settings
        val dynamicEnabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PREF_DYNAMIC_COLORS, true)

        if (dynamicEnabled) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }
}
