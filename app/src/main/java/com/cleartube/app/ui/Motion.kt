package com.cleartube.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

object Motion {
    /** Fast start, very long soft landing: the curve behind most of the screen transitions. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** Press feedback instead of a ripple: squishes under the finger, springs back with a little bounce. */
fun Modifier.bouncyClickable(pressedScale: Float = 0.95f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 600f),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** A moving light over a placeholder while something loads. */
fun Modifier.shimmer(): Modifier = composed {
    val sheen = pal.bg.copy(alpha = 0.45f)
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "shimmerX"
    )
    this.drawWithContent {
        drawContent()
        val w = size.width
        val c = x * w
        drawRect(
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, sheen, Color.Transparent),
                startX = c - w * 0.35f,
                endX = c + w * 0.35f
            )
        )
    }
}

/** Video-shaped placeholders shown over the page while it loads. */
@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
    val p = pal
    Column(
        modifier.fillMaxSize().clipToBounds().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        repeat(3) {
            Column {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(18.dp)).background(p.surface).shimmer()
                )
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(p.surface).shimmer())
                    Column(Modifier.padding(start = 12.dp)) {
                        Box(
                            Modifier.fillMaxWidth(0.85f).height(14.dp)
                                .clip(RoundedCornerShape(7.dp)).background(p.surface).shimmer()
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth(0.5f).height(12.dp)
                                .clip(RoundedCornerShape(6.dp)).background(p.surface).shimmer()
                        )
                    }
                }
            }
        }
    }
}
