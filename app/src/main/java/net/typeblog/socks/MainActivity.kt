package net.typeblog.socks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import net.typeblog.socks.util.Utility

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        Utility.extractFile(this)

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame, ProfileFragment())
            .commit()
    }
}
