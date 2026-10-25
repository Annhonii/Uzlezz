package com.cleartube.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TopTabsHeight = 64.dp

class TabSpec(val label: String, val icon: ImageVector, val url: String, val match: (String) -> Boolean)

/**
 * Tabs along the top: the open tab is a soft pill with icon + name (it grows with a spring), the others are plain
 * icons, the round button on the right is the account page.
 */
@Composable
fun TopTabBar(
    tabs: List<TabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = pal
    Row(
        modifier.fillMaxWidth().height(TopTabsHeight).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEachIndexed { i, t -> TopTabItem(t, i == selected) { onSelect(i) } }
        Box(
            Modifier.size(44.dp).bouncyClickable(0.9f, onAccount).clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PersonOutline, "Account", tint = p.muted, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun TopTabItem(t: TabSpec, selected: Boolean, onClick: () -> Unit) {
    val p = pal
    val tint = if (selected) p.accent else p.muted
    Row(
        Modifier
            .height(44.dp)
            .bouncyClickable(0.92f, onClick)
            .clip(CircleShape)
            .background(if (selected) p.pill else Color.Transparent)
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = 500f))
            .padding(horizontal = if (selected) 16.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(t.icon, t.label, tint = tint, modifier = Modifier.size(24.dp))
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(t.label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * Floating bottom bar (red logo + "Search"). Tapping it grows it into a text field with a spring; it slides away
 * while a video is open or in fullscreen.
 */
@Composable
fun FloatingSearch(
    visible: Boolean,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = pal
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    val widthFraction by animateFloatAsState(
        if (expanded) 1f else 0.88f, spring(dampingRatio = 0.8f, stiffness = 400f), label = "searchWidth"
    )
    LaunchedEffect(expanded) { if (!expanded) query = "" }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it } + fadeIn(tween(180)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(140))
    ) {
        Box(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val shape = RoundedCornerShape(30.dp)
            Box(
                Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(widthFraction)
                    .height(60.dp)
                    .shadow(10.dp, shape, ambientColor = Color.Black.copy(0.18f), spotColor = Color.Black.copy(0.18f))
                    .clip(shape)
                    .background(p.surface)
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                    label = "searchMorph"
                ) { open ->
                    if (!open) {
                        Row(
                            Modifier.fillMaxWidth().fillMaxHeight().bouncyClickable(0.98f) { onExpand(true) }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(width = 38.dp, height = 28.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE53935)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                "Search", color = p.muted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(start = 16.dp)
                            )
                            Icon(Icons.Rounded.Search, "Search", tint = p.text, modifier = Modifier.size(26.dp))
                        }
                    } else {
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focus.requestFocus() }
                        Row(
                            Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(44.dp).bouncyClickable(0.9f) { onExpand(false) }.clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.text, modifier = Modifier.size(24.dp))
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, fontFamily = Poppins),
                                cursorBrush = SolidColor(p.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (query.isNotBlank()) {
                                        onSubmit(query.trim())
                                        keyboard?.hide()
                                    }
                                }),
                                modifier = Modifier.weight(1f).padding(horizontal = 6.dp).focusRequester(focus),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (query.isEmpty()) Text("Search YouTube", color = p.muted, fontSize = 17.sp)
                                        inner()
                                    }
                                }
                            )
                            if (query.isNotEmpty()) {
                                Box(
                                    Modifier.size(44.dp).bouncyClickable(0.9f) { query = "" }.clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Close, "Clear", tint = p.muted, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Thin line under the tabs that fills while a page loads, then fades out. */
@Composable
fun LoadBar(progress: Int, loading: Boolean) {
    val p = pal
    val active = loading && progress < 100
    val fraction by animateFloatAsState(
        if (active) progress / 100f else 1f, tween(300, easing = Motion.Emphasized), label = "loadFraction"
    )
    val lineAlpha by animateFloatAsState(if (active) 1f else 0f, tween(250), label = "loadAlpha")
    Box(Modifier.fillMaxWidth().height(2.5.dp)) {
        Box(
            Modifier.fillMaxWidth(fraction).fillMaxHeight()
                .graphicsLayer { alpha = lineAlpha }
                .background(p.accent)
        )
    }
}
