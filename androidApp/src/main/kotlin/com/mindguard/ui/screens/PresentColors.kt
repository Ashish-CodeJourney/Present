package com.mindguard.ui.screens

import androidx.compose.ui.graphics.Color
import com.mindguard.ui.components.ChipTone
import com.mindguard.ui.theme.PresentPalette

internal object PresentColors {
    val Background: Color = PresentPalette.Cream
    val Ink: Color = PresentPalette.Ink
    val Primary: Color = PresentPalette.Sunrise
    val Muted: Color = PresentPalette.InkMuted
    val BlockedBg: Color = ChipTone.Blocked.background
    val BlockedFg: Color = ChipTone.Blocked.foreground
    val AllowedBg: Color = ChipTone.Allowed.background
    val AllowedFg: Color = ChipTone.Allowed.foreground
    val WebOnlyBg: Color = ChipTone.Neutral.background
    val WebOnlyFg: Color = ChipTone.Neutral.foreground
}
