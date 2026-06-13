package net.typeblog.socks;

import android.app.Application;
import android.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;

import static net.typeblog.socks.util.Constants.PREF_DYNAMIC_COLORS;

public class SocksApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply dynamic colors if enabled in settings
        boolean dynamicEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_DYNAMIC_COLORS, true);
        
        if (dynamicEnabled) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
