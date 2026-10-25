package com.cleartube.app.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cleartube.app.HOME
import com.cleartube.app.YtBrowser

/** Add, remove or re-point tabs here. `match` gets the page path (for example "/feed/subscriptions"). */
private val TABS = listOf(
    TabSpec("Home", Icons.Rounded.Home, HOME) { it == "/" || it.isEmpty() },
    TabSpec("Shorts", Icons.Rounded.SmartDisplay, "https://m.youtube.com/shorts") { it.startsWith("/shorts") },
    TabSpec("Subs", Icons.Rounded.Subscriptions, "https://m.youtube.com/feed/subscriptions") { it.startsWith("/feed/subscriptions") },
    TabSpec("Library", Icons.Rounded.VideoLibrary, "https://m.youtube.com/feed/library") {
        it.startsWith("/feed/library") || it.startsWith("/feed/history") || it.startsWith("/playlist")
    }
)

private const val ACCOUNT_URL = "https://m.youtube.com/feed/account"

@Composable
fun ClearTubeRoot(b: YtBrowser) {
    val p = pal
    val path = remember(b.url) { Uri.parse(b.url).path ?: "/" }
    val fullscreen = b.customView != null
    val onWatch = path.startsWith("/watch")
    val topVisible = !fullscreen && !onWatch
    val bottomVisible = topVisible && !path.startsWith("/shorts/")
    val selected = TABS.indexOfFirst { it.match(path) }
    var searching by remember { mutableStateOf(false) }

    // the tab row collapses upwards (and fades) while a video page is open
    val topHeight by animateDpAsState(
        if (topVisible) TopTabsHeight else 0.dp,
        spring(dampingRatio = 0.9f, stiffness = 400f), label = "topHeight"
    )
    // first load: skeleton first, then the page fades and slides in
    val showSkeleton = b.loading && b.progress < 60 && !onWatch
    val pageAlpha by animateFloatAsState(
        if (showSkeleton) 0f else 1f, tween(380, easing = Motion.Emphasized), label = "pageAlpha"
    )

    LaunchedEffect(p.dark) { b.applyTheme(p.dark, p.bg.toArgb()) }

    BackHandler(enabled = fullscreen || searching || b.canGoBack) {
        when {
            fullscreen -> b.exitFullscreen()
            searching -> searching = false
            else -> b.goBack()
        }
    }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(
                Modifier.fillMaxWidth().height(topHeight).clipToBounds()
                    .graphicsLayer { alpha = topHeight.value / TopTabsHeight.value }
            ) {
                TopTabBar(
                    tabs = TABS,
                    selected = selected,
                    onSelect = { i -> if (i != selected) b.open(TABS[i].url) },
                    onAccount = { b.open(ACCOUNT_URL) },
                    modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                )
            }
            LoadBar(b.progress, b.loading)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = {
                        (b.web.parent as? ViewGroup)?.removeView(b.web)
                        b.web
                    },
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = pageAlpha
                        translationY = (1f - pageAlpha) * 40f
                    }
                )
                // Keep the loading skeleton simple to avoid receiver-scope overload issues.
                if (showSkeleton) {
                    FeedSkeleton(Modifier.background(p.bg))
                }
            }
        }

        FloatingSearch(
            visible = bottomVisible,
            expanded = searching,
            onExpand = { searching = it },
            onSubmit = { q ->
                searching = false
                b.open("https://m.youtube.com/results?search_query=" + Uri.encode(q))
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // fullscreen video: the WebView gives us its video view, we show it over everything
        if (fullscreen) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { FrameLayout(it) },
                    update = { frame ->
                        frame.removeAllViews()
                        b.customView?.let { v ->
                            (v.parent as? ViewGroup)?.removeView(v)
                            frame.addView(v)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
