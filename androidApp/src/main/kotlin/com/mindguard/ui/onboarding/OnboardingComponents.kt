package com.mindguard.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.onboarding.formatMinutes
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val HOLD_TO_COMMIT_MS = 1500
private const val CONFETTI_MS = 1800

@Composable
fun NightSky(sunrise: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val stars = remember {
        val random = Random(42)
        List(46) { Triple(random.nextFloat(), random.nextFloat() * 0.7f, random.nextFloat()) }
    }
    val twinkle by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle-phase"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PresentPalette.nightGradient)
            .drawBehind {
                stars.forEach { (x, y, phase) ->
                    val alpha = (0.25f + 0.6f * kotlin.math.abs(twinkle - phase)) * (1f - sunrise * 0.7f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                        radius = (1f + phase * 1.6f) * density,
                        center = Offset(x * size.width, y * size.height)
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PresentPalette.Sunrise.copy(alpha = 0.55f * sunrise), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 1.05f),
                        radius = size.width * (0.6f + 0.6f * sunrise)
                    ),
                    radius = size.width * (0.6f + 0.6f * sunrise),
                    center = Offset(size.width / 2f, size.height * 1.05f)
                )
            }
    ) { content() }
}

@Composable
fun ProgressHeader(
    progress: Float,
    showProgress: Boolean,
    canGoBack: Boolean,
    canSkip: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val animated by animateFloatAsState(progress, PresentMotion.gentleSpring(), label = "progress")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.CenterStart) {
            if (canGoBack) HeaderAction("←", onBack)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (showProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(PresentShapes.Pill)
                        .background(PresentPalette.NightCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animated.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(PresentShapes.Pill)
                            .background(PresentPalette.sunriseGradient)
                    )
                }
            }
        }
        Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
            if (canSkip) HeaderAction("Skip", onSkip)
        }
    }
}

@Composable
private fun HeaderAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = if (text.length == 1) PresentType.Title else PresentType.Label,
        color = PresentPalette.OnNightMuted,
        modifier = Modifier
            .clip(PresentShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, PresentMotion.gentleSpring(), label = "press")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, tween(200), label = "enabled")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(PresentShapes.Pill)
            .background(PresentPalette.sunriseGradient)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = PresentType.Title, color = PresentPalette.Ink)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(PresentShapes.Pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = PresentType.Body, color = PresentPalette.OnNightMuted)
    }
}

@Composable
fun rememberEntrance(index: Int): Animatable<Float, *> {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * PresentMotion.StaggerMs.toLong())
        entrance.animateTo(1f, PresentMotion.gentleSpring())
    }
    return entrance
}

private fun Modifier.entrance(progress: Float, distancePx: Float): Modifier = graphicsLayer {
    alpha = progress.coerceIn(0f, 1f)
    translationY = (1f - progress) * distancePx
}

@Composable
private fun bounceOnSelect(selected: Boolean): Float {
    val bounce = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        bounce.snapTo(0.9f)
        bounce.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 600f))
    }
    return bounce.value
}

@Composable
fun SelectTile(
    emoji: String,
    label: String,
    selected: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entrance = rememberEntrance(index)
    val scale = bounceOnSelect(selected)
    val haptic = LocalHapticFeedback.current
    val distance = with(LocalDensity.current) { 32.dp.toPx() }
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, PresentMotion.gentleSpring(), label = "check")
    Box(
        modifier = modifier
            .entrance(entrance.value, distance)
            .scale(scale)
            .clip(PresentShapes.Card)
            .background(if (selected) PresentPalette.Sunrise.copy(alpha = 0.22f) else PresentPalette.NightCard)
            .border(2.dp, if (selected) PresentPalette.Sunrise else Color.Transparent, PresentShapes.Card)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 22.dp, horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = PresentType.Headline)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = PresentType.Title, color = PresentPalette.OnNight, textAlign = TextAlign.Center)
        }
        CheckBadge(scale = checkScale, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun CheckBadge(scale: Float, modifier: Modifier = Modifier) {
    if (scale <= 0.01f) return
    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(PresentPalette.Sunrise),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", style = PresentType.Label, color = PresentPalette.Ink)
    }
}

@Composable
fun OptionCard(
    label: String,
    selected: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null
) {
    val entrance = rememberEntrance(index)
    val scale = bounceOnSelect(selected)
    val haptic = LocalHapticFeedback.current
    val distance = with(LocalDensity.current) { 48.dp.toPx() }
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, PresentMotion.gentleSpring(), label = "check")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .entrance(entrance.value, distance)
            .scale(scale)
            .clip(PresentShapes.Card)
            .background(if (selected) PresentPalette.Sunrise.copy(alpha = 0.22f) else PresentPalette.NightCard)
            .border(2.dp, if (selected) PresentPalette.Sunrise else Color.Transparent, PresentShapes.Card)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(emoji, style = PresentType.Headline)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(label, style = PresentType.Title, color = PresentPalette.OnNight, modifier = Modifier.weight(1f))
        CheckBadge(scale = checkScale)
    }
}

@Composable
fun KeepChip(label: String, selected: Boolean, index: Int, onClick: () -> Unit) {
    val entrance = rememberEntrance(index)
    val scale = bounceOnSelect(selected)
    val haptic = LocalHapticFeedback.current
    val distance = with(LocalDensity.current) { 24.dp.toPx() }
    Row(
        modifier = Modifier
            .entrance(entrance.value, distance)
            .scale(scale)
            .clip(PresentShapes.Pill)
            .background(if (selected) PresentPalette.Mint else PresentPalette.NightCard)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selected) "✓  $label" else "+  $label",
            style = PresentType.Title,
            color = if (selected) PresentPalette.Ink else PresentPalette.OnNight
        )
    }
}

val SLIDER_MINUTES: List<Int> = listOf(15, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360)
private val SLIDER_EMOJI: List<String> = listOf("😌", "🙂", "🙂", "😐", "😕", "😟", "😩", "😵", "🥴", "😵‍💫", "😵‍💫")

fun sliderIndexFor(minutes: Int): Int =
    SLIDER_MINUTES.indices.minByOrNull { kotlin.math.abs(SLIDER_MINUTES[it] - minutes) } ?: 0

@Composable
fun EmojiSlider(minutes: Int, onMinutesChange: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val index = sliderIndexFor(minutes)
    val emojiScale = remember { Animatable(1f) }
    LaunchedEffect(index) {
        emojiScale.snapTo(1.25f)
        emojiScale.animateTo(1f, PresentMotion.gentleSpring())
    }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(SLIDER_EMOJI[index], style = PresentType.Display, modifier = Modifier.scale(emojiScale.value))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (index == SLIDER_MINUTES.lastIndex) "${formatMinutes(SLIDER_MINUTES[index])}+" else formatMinutes(SLIDER_MINUTES[index]),
            style = PresentType.Display,
            color = PresentPalette.OnNight
        )
        Text("a day", style = PresentType.Body, color = PresentPalette.OnNightMuted)
        Spacer(modifier = Modifier.height(24.dp))
        Slider(
            value = index.toFloat(),
            onValueChange = { raw ->
                val next = raw.roundToIntSafe().coerceIn(0, SLIDER_MINUTES.lastIndex)
                if (next != index) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMinutesChange(SLIDER_MINUTES[next])
                }
            },
            valueRange = 0f..SLIDER_MINUTES.lastIndex.toFloat(),
            steps = SLIDER_MINUTES.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = PresentPalette.Sunrise,
                activeTrackColor = PresentPalette.Sunrise,
                inactiveTrackColor = PresentPalette.NightCard,
                activeTickColor = PresentPalette.SunriseSoft,
                inactiveTickColor = PresentPalette.OnNightMuted
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("15 min", style = PresentType.Label, color = PresentPalette.OnNightMuted)
            Text("6 h+", style = PresentType.Label, color = PresentPalette.OnNightMuted)
        }
    }
}

private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

@Composable
fun CountUpText(
    target: Double,
    format: (Double) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    durationMillis: Int = 1200,
    textAlign: TextAlign? = null
) {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        delay(delayMillis.toLong())
        value.animateTo(target.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
    }
    Text(format(value.value.toDouble()), style = style, color = color, modifier = modifier, textAlign = textAlign)
}

@Composable
fun HoldToCommitButton(text: String, onCommitted: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val currentOnCommitted by rememberUpdatedState(onCommitted)
    var committed by remember { mutableStateOf(false) }
    val glow by rememberInfiniteTransition(label = "hold-glow").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "hold-glow-alpha"
    )
    Box(
        modifier = modifier
            .size(232.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (committed) return@detectTapGestures
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val fill = scope.launch {
                        val remaining = ((1f - progress.value) * HOLD_TO_COMMIT_MS).toInt().coerceAtLeast(1)
                        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
                        committed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnCommitted()
                    }
                    tryAwaitRelease()
                    if (!committed) {
                        fill.cancel()
                        scope.launch { progress.animateTo(0f, PresentMotion.gentleSpring()) }
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(
                brush = Brush.radialGradient(listOf(PresentPalette.Sunrise.copy(alpha = glow * 0.5f), Color.Transparent)),
                radius = size.minDimension / 2f
            )
            drawArc(
                color = PresentPalette.NightCard,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke)
            )
            drawArc(
                color = if (committed) PresentPalette.Mint else PresentPalette.Sunrise,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(184.dp)
                .scale(1f - 0.06f * progress.value)
                .clip(CircleShape)
                .background(if (committed) SolidColor(PresentPalette.Mint) else PresentPalette.sunriseGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (committed) "✓" else text,
                style = if (committed) PresentType.Display else PresentType.Title,
                color = PresentPalette.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

private data class ConfettiPiece(
    val vx: Float,
    val vy: Float,
    val spin: Float,
    val color: Color,
    val width: Float,
    val height: Float
)

@Composable
fun Confetti(play: Boolean, modifier: Modifier = Modifier) {
    val pieces = remember {
        val random = Random(7)
        val colors = listOf(PresentPalette.Sunrise, PresentPalette.Mint, PresentPalette.Coral, PresentPalette.SunriseSoft, Color.White)
        List(90) {
            ConfettiPiece(
                vx = random.nextFloat() * 2f - 1f,
                vy = -(0.7f + random.nextFloat() * 0.8f),
                spin = random.nextFloat() * 720f - 360f,
                color = colors[random.nextInt(colors.size)],
                width = 6f + random.nextFloat() * 6f,
                height = 10f + random.nextFloat() * 8f
            )
        }
    }
    val time = remember { Animatable(0f) }
    LaunchedEffect(play) {
        if (!play) return@LaunchedEffect
        time.snapTo(0f)
        time.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = time.value
        if (!play || t <= 0f || t >= 1f) return@Canvas
        val seconds = t * CONFETTI_MS / 1000f
        val origin = Offset(size.width / 2f, size.height * 0.5f)
        val gravity = size.height * 1.4f
        pieces.forEach { piece ->
            val x = origin.x + piece.vx * size.width * 0.55f * seconds
            val y = origin.y + piece.vy * size.height * 0.9f * seconds + 0.5f * gravity * seconds * seconds
            val center = Offset(x, y)
            rotate(degrees = piece.spin * seconds, pivot = center) {
                drawRect(
                    color = piece.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                    topLeft = Offset(x - piece.width * density / 2f, y - piece.height * density / 2f),
                    size = Size(piece.width * density, piece.height * density)
                )
            }
        }
    }
}

@Composable
fun StepTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = PresentType.Headline, color = PresentPalette.OnNight)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = PresentType.Body, color = PresentPalette.OnNightMuted)
        }
    }
}

@Composable
fun NightCardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PresentShapes.Card)
            .background(PresentPalette.NightCard)
            .padding(20.dp)
    ) { content() }
}
