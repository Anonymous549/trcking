package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SagePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = SageContainerLight,
    onPrimaryContainer = DeepMossLight,
    secondary = SoftTerracottaLight,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainerLight,
    onSecondaryContainer = Color(0xFF4A2512),
    tertiary = AmberGoldLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF463409),
    background = CreamBackgroundLight,
    onBackground = CharcoalInkLight,
    surface = IvorySurfaceLight,
    onSurface = CharcoalInkLight,
    surfaceVariant = OatContainerLight,
    onSurfaceVariant = SlateSecondaryLight,
    surfaceContainerLowest = IvorySurfaceLight,
    surfaceContainerLow = CreamBackgroundLight,
    surfaceContainer = OatContainerLight,
    surfaceContainerHigh = WarmSandLight,
    outline = HairlineBorderLight,
    outlineVariant = HairlineBorderLight,
    error = SoftErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF5A1916),
)

private val DarkColorScheme = darkColorScheme(
    primary = EucalyptusPrimaryDark,
    onPrimary = Color(0xFF0E2113),
    primaryContainer = EucalyptusContainerDark,
    onPrimaryContainer = Color(0xFFC5DFCA),
    secondary = TerracottaDark,
    onSecondary = Color(0xFF2F1407),
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = Color(0xFFF7D4C3),
    tertiary = WarmAmberDark,
    onTertiary = Color(0xFF281D04),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFF6E2B0),
    background = CharcoalBackgroundDark,
    onBackground = WarmCreamInkDark,
    surface = GraphiteSurfaceDark,
    onSurface = WarmCreamInkDark,
    surfaceVariant = ElevatedGraphiteDark,
    onSurfaceVariant = MutedStoneDark,
    surfaceContainerLowest = CharcoalBackgroundDark,
    surfaceContainerLow = GraphiteSurfaceDark,
    surfaceContainer = ElevatedGraphiteDark,
    surfaceContainerHigh = Color(0xFF303530),
    outline = HairlineBorderDark,
    outlineVariant = HairlineBorderDark,
    error = SoftErrorDark,
    onError = Color(0xFF380F0D),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFF8CCC9),
)

val LifeOsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

@Composable
fun LifeOsTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalLifeOsColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LifeOsTypography,
            shapes = LifeOsShapes,
            content = content,
        )
    }
}

object LifeOsThemeTokens {
    val colors: LifeOsExtendedColors
        @Composable get() = LocalLifeOsColors.current
}
