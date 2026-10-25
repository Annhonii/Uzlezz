package com.cleartube.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.ByteArrayInputStream

const val HOME = "https://m.youtube.com/"

/**
 * Owns the one WebView that shows m.youtube.com. The Compose UI only reads the small bits of state below
 * (current url, load progress, fullscreen video) and calls [open] / [goBack].
 */
class YtBrowser(private val activity: Activity) {

    var url by mutableStateOf(HOME)
    var progress by mutableIntStateOf(0)
    var loading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)

    /** Set while a video is fullscreen (the WebView hands us its video view). */
    var customView by mutableStateOf<View?>(null)
    var onFullscreen: (Boolean) -> Unit = {}

    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var documentStartInstalled = false

    // clean.css is put in front of clean.js as a JS string, so one script does everything
    private val script: String by lazy {
        fun asset(name: String) = activity.assets.open(name).bufferedReader().use { it.readText() }
        "window.__CT_CSS = " + JSONObject.quote(asset("clean.css")) + ";\n" + asset("clean.js")
    }

    private val chrome = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progress = newProgress
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            customCallback?.onCustomViewHidden()
            customCallback = callback
            customView = view
            onFullscreen(true)
        }

        override fun onHideCustomView() {
            customView = null
            customCallback?.onCustomViewHidden()
            customCallback = null
            onFullscreen(false)
        }

        // no grey placeholder picture before the video starts
        override fun getDefaultVideoPoster(): Bitmap =
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private val client = object : WebViewClient() {

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            if (isAdRequest(request.url)) {
                WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
            } else null

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val host = request.url.host ?: return false
            val ours = host.endsWith("youtube.com") || host == "youtu.be" || host.contains("google.") ||
                host.endsWith("gstatic.com") || host.endsWith("googleusercontent.com") || host.endsWith("ytimg.com")
            if (ours) return false
            // any other site opens in the phone's browser
            try { activity.startActivity(Intent(Intent.ACTION_VIEW, request.url)) } catch (_: Exception) {}
            return true
        }

        override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) {
            loading = true
            progress = 0
            if (!documentStartInstalled) view.evaluateJavascript(script, null)
        }

        override fun onPageFinished(view: WebView, pageUrl: String?) {
            loading = false
            canGoBack = view.canGoBack()
        }

        // also fires for YouTube's in-page navigation (it never reloads the page when you open a video)
        override fun doUpdateVisitedHistory(view: WebView, visited: String?, isReload: Boolean) {
            if (visited != null) url = visited
            canGoBack = view.canGoBack()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    val web: WebView = WebView(activity).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setSupportZoom(false)
        // look like Chrome: Google's sign-in page refuses WebViews ("; wv" in the user agent)
        settings.userAgentString = settings.userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }

    init {
        setup()
    }

    private fun setup() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, script, setOf("https://m.youtube.com"))
            documentStartInstalled = true
        }
        web.webViewClient = client
        web.webChromeClient = chrome
    }

    fun open(target: String) {
        web.loadUrl(target)
    }

    fun goBack() {
        if (web.canGoBack()) web.goBack()
    }

    fun exitFullscreen() = chrome.onHideCustomView()

    fun applyTheme(dark: Boolean, backgroundArgb: Int) {
        web.setBackgroundColor(backgroundArgb)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(web.settings, dark)
        }
    }

    fun destroy() {
        web.stopLoading()
        web.destroy()
    }

    private fun isAdRequest(u: Uri): Boolean {
        val host = u.host ?: return false
        val path = u.path.orEmpty()
        if (host.endsWith("doubleclick.net") || host.endsWith("googlesyndication.com") ||
            host.endsWith("googleadservices.com") || host == "adservice.google.com"
        ) return true
        return host.endsWith("youtube.com") &&
            (path.startsWith("/pagead/") || path.startsWith("/api/stats/ads") ||
                path.startsWith("/ptracking") || path.startsWith("/get_midroll_"))
    }
}
