package com.mindguard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
fun Staggered(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.toLong() * PresentMotion.StaggerMs)
        progress.animateTo(1f, tween(durationMillis = PresentMotion.EnterMs, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 28.dp.toPx()
        }
    ) {
        content()
    }
}

@Composable
fun rememberCountUp(target: Long, durationMs: Int = 900): Long {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(durationMillis = durationMs, easing = FastOutSlowInEasing))
    }
    return value.value.roundToLong()
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 14.dp,
    color: Color = PresentPalette.Sunrise,
    trackColor: Color = PresentPalette.OnNight.copy(alpha = 0.12f),
    glow: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = Offset(stroke, stroke)
            val ringSize = Size(this.size.width - stroke * 2, this.size.height - stroke * 2)
            val sweep = 360f * progress.coerceIn(0f, 1f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = inset,
                size = ringSize,
                style = Stroke(width = stroke)
            )
            if (glow && sweep > 0f) {
                drawArc(
                    color = color.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = inset,
                    size = ringSize,
                    style = Stroke(width = stroke * 2f, cap = StrokeCap.Round)
                )
            }
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = inset,
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val pulse by rememberInfiniteTransition(label = "pulseDot").animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulseScale"
    )
    Box(modifier = modifier.size(size * 2.2f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    alpha = (2.2f - pulse) / 1.2f * 0.5f
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 14.dp,
    base: Color = PresentPalette.SunriseSoft.copy(alpha = 0.6f),
    highlight: Color = PresentPalette.CardLight
) {
    val shift by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1300, easing = LinearEasing)),
        label = "shimmerShift"
    )
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(PresentShapes.Pill)
            .drawBehind {
                val center = shift * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(center - size.width * 0.4f, 0f),
                        end = Offset(center + size.width * 0.4f, 0f)
                    )
                )
            }
    )
}
