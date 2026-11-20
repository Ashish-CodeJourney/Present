package com.mindguard.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mindguard.ui.theme.PresentPalette

private val FieldBorder = Color(0xFFEDE3D8)

@Composable
fun presentTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = PresentPalette.CardLight,
    unfocusedContainerColor = PresentPalette.CardLight,
    disabledContainerColor = PresentPalette.CardLight,
    focusedBorderColor = PresentPalette.Sunrise,
    unfocusedBorderColor = FieldBorder,
    disabledBorderColor = FieldBorder,
    focusedTextColor = PresentPalette.Ink,
    unfocusedTextColor = PresentPalette.Ink,
    disabledTextColor = PresentPalette.InkMuted,
    cursorColor = PresentPalette.Sunrise,
    focusedLabelColor = PresentPalette.Sunrise,
    unfocusedLabelColor = PresentPalette.InkMuted,
    focusedPlaceholderColor = PresentPalette.InkMuted,
    unfocusedPlaceholderColor = PresentPalette.InkMuted,
    focusedSupportingTextColor = PresentPalette.InkMuted,
    unfocusedSupportingTextColor = PresentPalette.InkMuted
)
