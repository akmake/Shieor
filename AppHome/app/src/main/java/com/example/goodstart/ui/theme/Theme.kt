package com.example.goodstart.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.example.goodstart.R

val Primary     = Color(0xFF0f766e)
val PrimaryLight= Color(0xFF14b8a6)
val Ink         = Color(0xFF1a1a2e)
val Muted       = Color(0xFF6b7280)
val BgColor     = Color(0xFFF8F6F0)
val CardBg      = Color(0xFFFFFFFF)
val LineColor   = Color(0xFFE5E7EB)
val Amber       = Color(0xFFb58900)

val AccentBlue   = Color(0xFF3b82f6)
val AccentEmerald= Color(0xFF10b981)
val AccentViolet = Color(0xFF8b5cf6)
val AccentAmber  = Color(0xFFf59e0b)

val SblHebrew   = FontFamily(Font(R.font.sbl_hbrw))
val BaHaYetzira = FontFamily(Font(R.font.ba_hayetzira_regular))

private val ColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = Color.White,
    background       = BgColor,
    surface          = CardBg,
    onBackground     = Ink,
    onSurface        = Ink,
    secondary        = PrimaryLight,
    surfaceVariant   = Color(0xFFF1F5F9),
    outline          = LineColor,
)

@Composable
fun ShieorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}

fun accentColor(accent: String?): Color = when (accent) {
    "emerald" -> AccentEmerald
    "violet"  -> AccentViolet
    "amber"   -> AccentAmber
    else      -> AccentBlue
}
