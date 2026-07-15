package net.typeblog.socks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.preference.PreferenceManager
import net.typeblog.socks.ui.navigation.AppNavigation
import net.typeblog.socks.ui.theme.KiloProxyTheme
import net.typeblog.socks.util.Constants.PREF_THEME_MODE
import net.typeblog.socks.util.Utility

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val themeMode = prefs.getString(PREF_THEME_MODE, "light")
        // theme will be applied by KiloProxyTheme composable

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KiloProxyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }

        // Startup IP check
        Thread {
            val ipInfo = Utility.checkPublicIp()
            ipInfo // stored in ViewModel
        }.start()
    }
}
