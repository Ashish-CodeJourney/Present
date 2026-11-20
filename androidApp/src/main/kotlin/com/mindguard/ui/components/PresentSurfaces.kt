package com.mindguard.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val radius: Float, val baseAlpha: Float)

private const val STAR_COUNT = 34

@Composable
fun PresentScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PresentPalette.Cream)
            .then(scrollModifier)
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

@Composable
fun PresentCard(
    modifier: Modifier = Modifier,
    color: Color = PresentPalette.CardLight,
    border: BorderStroke? = null,
    contentPadding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = PresentShapes.Card
    val borderModifier = if (border != null) Modifier.border(border, shape) else Modifier
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = shape, ambientColor = PresentPalette.Ink.copy(alpha = 0.08f), spotColor = PresentPalette.Ink.copy(alpha = 0.10f))
            .clip(shape)
            .background(color)
            .then(borderModifier)
            .then(clickModifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun NightSurface(
    modifier: Modifier = Modifier,
    shape: Shape = PresentShapes.Card,
    sunriseGlow: Boolean = true,
    contentPadding: Dp = 24.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PresentPalette.nightGradient)
    ) {
        StarField(modifier = Modifier.matchParentSize(), sunriseGlow = sunriseGlow)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

@Composable
fun StarField(modifier: Modifier = Modifier, sunriseGlow: Boolean = true, seed: Int = 7) {
    val stars = remember(seed) {
        val random = Random(seed)
        List(STAR_COUNT) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat() * 0.85f,
                radius = 0.6f + random.nextFloat() * 1.4f,
                baseAlpha = 0.35f + random.nextFloat() * 0.6f
            )
        }
    }
    val twinkle by rememberInfiniteTransition(label = "stars").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600), RepeatMode.Reverse),
        label = "twinkle"
    )
    Canvas(modifier = modifier) {
        if (sunriseGlow) {
            val glowCenter = Offset(size.width * 0.82f, size.height * 1.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PresentPalette.Sunrise.copy(alpha = 0.42f), Color.Transparent),
                    center = glowCenter,
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = glowCenter
            )
        }
        stars.forEachIndexed { index, star ->
            val phase = if (index % 2 == 0) twinkle else 1f - twinkle
            drawCircle(
                color = PresentPalette.OnNight,
                radius = star.radius * density,
                center = Offset(star.x * size.width, star.y * size.height),
                alpha = star.baseAlpha * (0.45f + 0.55f * phase)
            )
        }
    }
}

@Composable
fun EmojiBadge(
    emoji: String,
    modifier: Modifier = Modifier,
    background: Color = PresentPalette.SunriseSoft,
    size: Dp = 48.dp,
    style: androidx.compose.ui.text.TextStyle = PresentType.Title
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = style, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = PresentPalette.InkMuted) {
    Text(
        text.uppercase(),
        style = PresentType.Label,
        color = color,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, backEnabled: Boolean = true) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackPill(onClick = onBack, enabled = backEnabled)
        Spacer(modifier = Modifier.weight(1f))
        Text(title, style = PresentType.Title, color = PresentPalette.Ink)
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(width = 76.dp, height = 1.dp))
    }
}

@Composable
fun VerticalGap(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}
