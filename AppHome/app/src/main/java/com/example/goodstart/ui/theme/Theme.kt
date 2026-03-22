package com.example.goodstart.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.goodstart.R

val Primary     = Color(0xFF0f766e)
val PrimaryLight= Color(0xFF14b8a6)
val Ink         = Color(0xFF1a1a2e)
val Muted       = Color(0xFF6b7280)
val BgColor     = Color(0xFFF8F6F0)
val CardBg      = Color(0xFFFFFFFF)
val LineColor   = Color(0xFFE5E7EB)
val Amber       = Color(0xFFb58900)

val SblHebrew   = FontFamily(Font(R.font.sbl_hbrw))
val BaHaYetzira = FontFamily(Font(R.font.ba_hayetzira_regular))

// Defining typography to use SblHebrew as the default for everything
private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = SblHebrew),
    displayMedium = TextStyle(fontFamily = SblHebrew),
    displaySmall = TextStyle(fontFamily = SblHebrew),
    headlineLarge = TextStyle(fontFamily = SblHebrew),
    headlineMedium = TextStyle(fontFamily = SblHebrew),
    headlineSmall = TextStyle(fontFamily = SblHebrew),
    titleLarge = TextStyle(fontFamily = SblHebrew),
    titleMedium = TextStyle(fontFamily = SblHebrew),
    titleSmall = TextStyle(fontFamily = SblHebrew),
    bodyLarge = TextStyle(fontFamily = SblHebrew),
    bodyMedium = TextStyle(fontFamily = SblHebrew),
    bodySmall = TextStyle(fontFamily = SblHebrew),
    labelLarge = TextStyle(fontFamily = SblHebrew),
    labelMedium = TextStyle(fontFamily = SblHebrew),
    labelSmall = TextStyle(fontFamily = SblHebrew)
)

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
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = AppTypography
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}
