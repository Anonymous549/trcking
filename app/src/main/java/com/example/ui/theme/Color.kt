package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Brand Palette — Warm Cream, Charcoal & Muted Sage Green
val CreamBackgroundLight = Color(0xFFF7F4EE)
val IvorySurfaceLight = Color(0xFFFFFDF9)
val OatContainerLight = Color(0xFFEFECE3)
val WarmSandLight = Color(0xFFE5E0D3)
val CharcoalInkLight = Color(0xFF1E201D)
val SlateSecondaryLight = Color(0xFF5C615A)
val HairlineBorderLight = Color(0xFFDDD7C9)

val SagePrimaryLight = Color(0xFF4A6B53)
val DeepMossLight = Color(0xFF2F4836)
val SageContainerLight = Color(0xFFDDE8DF)
val SoftTerracottaLight = Color(0xFFC47A53)
val TerracottaContainerLight = Color(0xFFF6E3D8)
val AmberGoldLight = Color(0xFFC89B3C)
val AmberContainerLight = Color(0xFFF6ECD2)
val SoftErrorLight = Color(0xFFB34842)
val ErrorContainerLight = Color(0xFFF9DFDD)

// Dark Mode Palette — Deep Charcoal, Graphite & Eucalyptus Sage
val CharcoalBackgroundDark = Color(0xFF141614)
val GraphiteSurfaceDark = Color(0xFF1E211E)
val ElevatedGraphiteDark = Color(0xFF272B27)
val HairlineBorderDark = Color(0xFF333833)
val WarmCreamInkDark = Color(0xFFECE9E0)
val MutedStoneDark = Color(0xFFA7ACA3)

val EucalyptusPrimaryDark = Color(0xFF7FA889)
val EucalyptusContainerDark = Color(0xFF27392B)
val WarmAmberDark = Color(0xFFE0B65B)
val AmberContainerDark = Color(0xFF3C311C)
val TerracottaDark = Color(0xFFDF936C)
val TerracottaContainerDark = Color(0xFF3D271E)
val SoftErrorDark = Color(0xFFE57B74)
val ErrorContainerDark = Color(0xFF442220)

@Immutable
data class LifeOsExtendedColors(
    val border: Color,
    val positive: Color,
    val positiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val accent: Color,
    val accentContainer: Color,
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,
    val chartQuaternary: Color,
    val heatmapEmpty: Color,
    val heatmapLevel1: Color,
    val heatmapLevel2: Color,
    val heatmapLevel3: Color,
    val heatmapLevel4: Color,
)

val LightExtendedColors = LifeOsExtendedColors(
    border = HairlineBorderLight,
    positive = SagePrimaryLight,
    positiveContainer = SageContainerLight,
    warning = AmberGoldLight,
    warningContainer = AmberContainerLight,
    accent = SoftTerracottaLight,
    accentContainer = TerracottaContainerLight,
    chartPrimary = SagePrimaryLight,
    chartSecondary = SoftTerracottaLight,
    chartTertiary = AmberGoldLight,
    chartQuaternary = Color(0xFF607D8B),
    heatmapEmpty = Color(0xFFEAE5D9),
    heatmapLevel1 = Color(0xFFC2D4C6),
    heatmapLevel2 = Color(0xFF8EB096),
    heatmapLevel3 = Color(0xFF5D8768),
    heatmapLevel4 = Color(0xFF385C42),
)

val DarkExtendedColors = LifeOsExtendedColors(
    border = HairlineBorderDark,
    positive = EucalyptusPrimaryDark,
    positiveContainer = EucalyptusContainerDark,
    warning = WarmAmberDark,
    warningContainer = AmberContainerDark,
    accent = TerracottaDark,
    accentContainer = TerracottaContainerDark,
    chartPrimary = EucalyptusPrimaryDark,
    chartSecondary = TerracottaDark,
    chartTertiary = WarmAmberDark,
    chartQuaternary = Color(0xFF8AA8B8),
    heatmapEmpty = Color(0xFF282C28),
    heatmapLevel1 = Color(0xFF2B3E30),
    heatmapLevel2 = Color(0xFF3D5C45),
    heatmapLevel3 = Color(0xFF5A8465),
    heatmapLevel4 = Color(0xFF84B290),
)

val LocalLifeOsColors = staticCompositionLocalOf { LightExtendedColors }
