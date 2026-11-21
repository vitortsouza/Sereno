package com.opencode.sereno.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.opencode.sereno.data.ThemeMode

data class SerenoPalette(
    val Bg: Color,
    val Surface: Color,
    val SurfaceHi: Color,
    val SurfaceSoft: Color,
    val Mint: Color,
    val MintDeep: Color,
    val Text: Color,
    val Muted: Color,
    val Outline: Color,
    val Warm: Color,
    val Nature: Color,
    val Noise: Color,
    val Binaural: Color,
    val Frequency: Color,
    val Music: Color,
    val Mix: Color
)

private val DarkPalette = SerenoPalette(
    Bg = Color(0xFF070D0B),
    Surface = Color(0xFF101815),
    SurfaceHi = Color(0xFF16211D),
    SurfaceSoft = Color(0xFF1B2823),
    Mint = Color(0xFF9FE3CF),
    MintDeep = Color(0xFF4E8C7E),
    Text = Color(0xFFE9F2EE),
    Muted = Color(0xFF8CA098),
    Outline = Color(0xFF24332D),
    Warm = Color(0xFFE3C08A),
    Nature = Color(0xFF8FD6C2),
    Noise = Color(0xFF9DB0C4),
    Binaural = Color(0xFF86BEDD),
    Frequency = Color(0xFFDCC08A),
    Music = Color(0xFFBCA6DE),
    Mix = Color(0xFFCFA9A0)
)

private val LightPalette = SerenoPalette(
    Bg = Color(0xFFF4F6F2),
    Surface = Color(0xFFFBFCFA),
    SurfaceHi = Color(0xFFEDF1EC),
    SurfaceSoft = Color(0xFFE4EBE5),
    Mint = Color(0xFF2E7A66),
    MintDeep = Color(0xFF225C4D),
    Text = Color(0xFF14211C),
    Muted = Color(0xFF5D7169),
    Outline = Color(0xFFD3DED6),
    Warm = Color(0xFF8A6534),
    Nature = Color(0xFF2F7C68),
    Noise = Color(0xFF4A657C),
    Binaural = Color(0xFF2D7096),
    Frequency = Color(0xFF7A6326),
    Music = Color(0xFF6C5591),
    Mix = Color(0xFF8B6056)
)

private val LocalSerenoPalette = staticCompositionLocalOf { DarkPalette }

object SerenoColors {
    val Bg: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Bg
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Surface
    val SurfaceHi: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.SurfaceHi
    val SurfaceSoft: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.SurfaceSoft
    val Mint: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Mint
    val MintDeep: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.MintDeep
    val Text: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Text
    val Muted: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Muted
    val Outline: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Outline
    val Warm: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Warm
    val Nature: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Nature
    val Noise: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Noise
    val Binaural: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Binaural
    val Frequency: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Frequency
    val Music: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Music
    val Mix: Color
        @Composable @ReadOnlyComposable get() = LocalSerenoPalette.current.Mix
}

private val DarkScheme = darkColorScheme(
    primary = DarkPalette.Mint,
    onPrimary = Color(0xFF06201A),
    primaryContainer = Color(0xFF1D3A32),
    onPrimaryContainer = DarkPalette.Mint,
    secondary = DarkPalette.MintDeep,
    onSecondary = Color(0xFF04120E),
    background = DarkPalette.Bg,
    onBackground = DarkPalette.Text,
    surface = DarkPalette.Surface,
    onSurface = DarkPalette.Text,
    surfaceVariant = DarkPalette.SurfaceHi,
    onSurfaceVariant = DarkPalette.Muted,
    outline = DarkPalette.Outline,
    outlineVariant = DarkPalette.Outline,
    error = Color(0xFFE8A08F)
)

private val LightScheme = lightColorScheme(
    primary = LightPalette.Mint,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEE6),
    onPrimaryContainer = Color(0xFF0A3028),
    secondary = LightPalette.MintDeep,
    onSecondary = Color.White,
    background = LightPalette.Bg,
    onBackground = LightPalette.Text,
    surface = LightPalette.Surface,
    onSurface = LightPalette.Text,
    surfaceVariant = LightPalette.SurfaceHi,
    onSurfaceVariant = LightPalette.Muted,
    outline = LightPalette.Outline,
    outlineVariant = LightPalette.Outline,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val SerenoType = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light,
        fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp
    )
)

fun isDarkMode(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun SerenoTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = isDarkMode(mode, isSystemInDarkTheme())
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalSerenoPalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = SerenoType,
            content = content
        )
    }
}
