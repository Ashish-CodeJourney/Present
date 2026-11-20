package com.mindguard.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType

private val DisabledFill = Color(0xFFEDE6DE)

@Composable
private fun PillBase(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    fill: Brush?,
    borderColor: Color?,
    height: Dp,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = PresentMotion.gentleSpring(),
        label = "pillPress"
    )
    val fillModifier = if (fill != null) Modifier.background(fill) else Modifier
    val borderModifier = if (borderColor != null) Modifier.border(1.5.dp, borderColor, PresentShapes.Pill) else Modifier
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PresentShapes.Pill)
            .then(fillModifier)
            .then(borderModifier)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun SunrisePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = text,
    height: Dp = 56.dp
) {
    val pulse by rememberInfiniteTransition(label = "pillPulse").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "pillPulseAlpha"
    )
    val active = enabled || loading
    val fill = if (active) PresentPalette.sunriseGradient else SolidColor(DisabledFill)
    PillBase(
        onClick = onClick,
        modifier = modifier.graphicsLayer { alpha = if (loading) pulse else 1f },
        enabled = enabled && !loading,
        fill = fill,
        borderColor = null,
        height = height
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                if (loading) loadingText else text,
                style = PresentType.Title.copy(fontSize = PresentType.Body.fontSize),
                color = if (active) Color.White else PresentPalette.InkMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SolidPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = PresentPalette.Ink,
    contentColor: Color = Color.White,
    height: Dp = 52.dp
) {
    PillBase(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fill = SolidColor(if (enabled) container else DisabledFill),
        borderColor = null,
        height = height
    ) {
        Text(
            text,
            style = PresentType.Title.copy(fontSize = PresentType.Body.fontSize),
            color = if (enabled) contentColor else PresentPalette.InkMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = PresentPalette.Ink,
    height: Dp = 52.dp
) {
    val tint = if (enabled) color else PresentPalette.InkMuted.copy(alpha = 0.5f)
    PillBase(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fill = null,
        borderColor = tint.copy(alpha = 0.35f),
        height = height
    ) {
        Text(text, style = PresentType.Body, color = tint, textAlign = TextAlign.Center)
    }
}

@Composable
fun BackPill(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    PillBase(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fill = SolidColor(PresentPalette.CardLight),
        borderColor = null,
        height = 40.dp
    ) {
        Text(
            "← Back",
            style = PresentType.Label,
            color = if (enabled) PresentPalette.Ink else PresentPalette.InkMuted
        )
    }
}
