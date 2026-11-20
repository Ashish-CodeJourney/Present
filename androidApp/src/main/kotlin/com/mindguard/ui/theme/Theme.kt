package com.mindguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PresentColorScheme = lightColorScheme(
    primary = PresentPalette.Sunrise,
    onPrimary = Color.White,
    primaryContainer = PresentPalette.SunriseSoft,
    onPrimaryContainer = PresentPalette.Ink,
    secondary = PresentPalette.Mint,
    onSecondary = PresentPalette.Ink,
    secondaryContainer = PresentPalette.MintSoft,
    onSecondaryContainer = PresentPalette.Ink,
    tertiary = PresentPalette.NightBottom,
    onTertiary = PresentPalette.OnNight,
    background = PresentPalette.Cream,
    onBackground = PresentPalette.Ink,
    surface = PresentPalette.CardLight,
    onSurface = PresentPalette.Ink,
    surfaceVariant = PresentPalette.Cream,
    onSurfaceVariant = PresentPalette.InkMuted,
    outline = PresentPalette.InkMuted,
    error = PresentPalette.Coral,
    onError = Color.White,
    errorContainer = PresentPalette.CoralSoft,
    onErrorContainer = PresentPalette.Ink
)

private val PresentTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = PresentDisplayFont),
        displayMedium = base.displayMedium.copy(fontFamily = PresentDisplayFont),
        displaySmall = base.displaySmall.copy(fontFamily = PresentDisplayFont),
        headlineLarge = PresentType.Headline,
        headlineMedium = base.headlineMedium.copy(fontFamily = PresentDisplayFont),
        headlineSmall = base.headlineSmall.copy(fontFamily = PresentDisplayFont),
        titleLarge = PresentType.Title,
        titleMedium = base.titleMedium.copy(fontFamily = PresentDisplayFont),
        titleSmall = base.titleSmall.copy(fontFamily = PresentDisplayFont),
        bodyLarge = PresentType.Body,
        bodyMedium = base.bodyMedium.copy(fontFamily = PresentDisplayFont),
        bodySmall = base.bodySmall.copy(fontFamily = PresentDisplayFont),
        labelLarge = base.labelLarge.copy(fontFamily = PresentDisplayFont),
        labelMedium = PresentType.Label,
        labelSmall = base.labelSmall.copy(fontFamily = PresentDisplayFont)
    )
}

@Composable
fun PresentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PresentColorScheme,
        typography = PresentTypography,
        content = content
    )
}

@Composable
fun MindGuardTheme(content: @Composable () -> Unit) = PresentTheme(content)
