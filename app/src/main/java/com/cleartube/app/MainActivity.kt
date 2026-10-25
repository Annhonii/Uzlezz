package com.cleartube.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cleartube.app.ui.ClearTubeRoot
import com.cleartube.app.ui.ClearTubeTheme

class MainActivity : ComponentActivity() {

    private lateinit var browser: YtBrowser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browser = YtBrowser(this)
        browser.onFullscreen = { on -> applyFullscreen(on) }
        browser.open(urlFrom(intent) ?: HOME)

        setContent {
            // transparent bars; the icons turn dark / light with the system theme (the activity is not
            // recreated on a theme switch, so this is redone whenever it changes)
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                )
                onDispose { }
            }
            ClearTubeTheme { ClearTubeRoot(browser) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        urlFrom(intent)?.let { browser.open(it) }
    }

    override fun onResume() {
        super.onResume()
        browser.web.onResume()
    }

    override fun onDestroy() {
        browser.destroy()
        super.onDestroy()
    }

    /** Shared / tapped YouTube links open inside the app, on the mobile site. */
    private fun urlFrom(i: Intent?): String? {
        val raw = when (i?.action) {
            Intent.ACTION_VIEW -> i.dataString
            Intent.ACTION_SEND -> Regex("https?://\\S+").find(i.getStringExtra(Intent.EXTRA_TEXT).orEmpty())?.value
            else -> null
        } ?: return null
        val u = Uri.parse(raw)
        val host = u.host ?: return null
        return when {
            host == "youtu.be" -> "https://m.youtube.com/watch?v=" + (u.lastPathSegment ?: return null)
            host.endsWith("youtube.com") -> u.buildUpon().scheme("https").authority("m.youtube.com").build().toString()
            else -> null
        }
    }

    private fun applyFullscreen(on: Boolean) {
        requestedOrientation =
            if (on) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
