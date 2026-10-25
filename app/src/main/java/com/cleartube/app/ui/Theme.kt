package com.cleartube.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cleartube.app.R

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/** Same black / white palette as Cleantube: soft grey-black in dark, red only for the logo. */
@Immutable
class Pal(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,   // floating bar, skeleton blocks
    val text: Color,
    val muted: Color,
    val accent: Color,    // selected tab, progress line
    val pill: Color       // soft background behind the selected tab
)

val LightPal = Pal(false, Color.White, Color(0xFFE6E6E6), Color.Black, Color(0xFF606060), Color.Black, Color(0x1F000000))
val DarkPal = Pal(true, Color(0xFF121212), Color(0xFF2E2E2E), Color.White, Color(0xFFAAAAAA), Color.White, Color(0x29FFFFFF))

val LocalPal = staticCompositionLocalOf { LightPal }

val pal: Pal
    @Composable @ReadOnlyComposable get() = LocalPal.current

@Composable
fun ClearTubeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPal else LightPal
    val scheme = if (dark) {
        darkColorScheme(primary = p.accent, onPrimary = p.bg, background = p.bg, surface = p.bg, onSurface = p.text)
    } else {
        lightColorScheme(primary = p.accent, onPrimary = p.bg, background = p.bg, surface = p.bg, onSurface = p.text)
    }
    CompositionLocalProvider(LocalPal provides p) {
        MaterialTheme(colorScheme = scheme) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = Poppins))
            ) { content() }
        }
    }
}
