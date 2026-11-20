package com.mindguard.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mindguard.onboarding.DEFAULT_AGE
import com.mindguard.onboarding.OnboardingAnswers
import com.mindguard.onboarding.Projection
import com.mindguard.onboarding.RECLAIM_FACTOR
import com.mindguard.onboarding.formatMinutes
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

private const val BUILD_ROW_MS = 650L

private val BUILD_ROWS = listOf(
    "Reading your answers",
    "Estimating your Reels & Shorts time",
    "Matching real-world wins",
    "Drafting your plan"
)

@Composable
fun BuildingStep(onDone: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var done by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        BUILD_ROWS.indices.forEach { _ ->
            delay(BUILD_ROW_MS)
            done += 1
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        delay(300)
        onDone()
    }
    StepLayout {
        Spacer(modifier = Modifier.height(48.dp))
        StepTitle("Building your plan…", "Personal to you. Takes a few seconds.")
        Spacer(modifier = Modifier.height(32.dp))
        BUILD_ROWS.forEachIndexed { index, label ->
            val ticked = index < done
            val tickScale by animateFloatAsState(if (ticked) 1f else 0f, PresentMotion.gentleSpring(), label = "tick")
            Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    if (index == done) {
                        CircularProgressIndicator(color = PresentPalette.Sunrise, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .scale(tickScale)
                            .clip(CircleShape)
                            .background(PresentPalette.Mint),
                        contentAlignment = Alignment.Center
                    ) { Text("✓", style = PresentType.Label, color = PresentPalette.Ink) }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    label,
                    style = PresentType.Title,
                    color = if (ticked || index == done) PresentPalette.OnNight else PresentPalette.OnNightMuted
                )
            }
        }
    }
}

private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

@Composable
private fun rememberRevealed(count: Int, stepMs: Long): Int {
    var revealed by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(count) {
            revealed += 1
            delay(stepMs)
        }
    }
    return revealed
}

@Composable
fun MirrorStep(
    answers: OnboardingAnswers,
    projection: Projection,
    onShare: () -> Unit,
    onContinue: () -> Unit
) {
    val revealed = rememberRevealed(count = 4, stepMs = 1300)
    var showFormula by remember { mutableStateOf(false) }
    StepLayout(footer = {
        AnimatedVisibility(visible = revealed >= 4, enter = fadeIn() + slideInVertically { it / 2 }) {
            Column {
                PrimaryButton("There's a better way →", onContinue)
                GhostButton("Share this", onShare)
            }
        }
    }) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (answers.isMeasured) "Measured from your phone" else "Based on your answers",
            style = PresentType.Label,
            color = PresentPalette.OnNightMuted
        )
        Spacer(modifier = Modifier.height(16.dp))
        RevealLine(visible = revealed >= 1) {
            CountUpText(
                target = projection.dailyShortFormMinutes.toDouble(),
                format = { formatMinutes(it.roundToInt()) },
                style = PresentType.Display,
                color = PresentPalette.OnNight
            )
            Text("a day on Reels & Shorts", style = PresentType.Title, color = PresentPalette.OnNightMuted)
        }
        Spacer(modifier = Modifier.height(24.dp))
        RevealLine(visible = revealed >= 2) {
            CountUpText(
                target = projection.yearlyDays.toDouble(),
                format = { "${it.roundToInt()} days" },
                style = PresentType.Display,
                color = PresentPalette.OnNight
            )
            Text("a year. Every year.", style = PresentType.Title, color = PresentPalette.OnNightMuted)
        }
        Spacer(modifier = Modifier.height(24.dp))
        RevealLine(visible = revealed >= 3) {
            CountUpText(
                target = projection.lifetimeYears,
                format = { "${oneDecimal(it)} years" },
                style = PresentType.Display,
                color = PresentPalette.Sunrise,
                durationMillis = 1600
            )
            Text("of your waking life", style = PresentType.Title, color = PresentPalette.OnNight)
        }
        Spacer(modifier = Modifier.height(20.dp))
        RevealLine(visible = revealed >= 4) {
            Text(
                "This happens to almost everyone. The feed is built to do it.",
                style = PresentType.Body,
                color = PresentPalette.OnNightMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (showFormula) "estimate · how we calculate ▴" else "estimate · how we calculate ▾",
                style = PresentType.Label,
                color = PresentPalette.OnNightMuted,
                modifier = Modifier
                    .clip(PresentShapes.Pill)
                    .clickable { showFormula = !showFormula }
                    .padding(vertical = 6.dp)
            )
            AnimatedVisibility(visible = showFormula, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                NightCardBox {
                    Text(
                        formulaText(answers, projection),
                        style = PresentType.Label,
                        color = PresentPalette.OnNight
                    )
                }
            }
        }
    }
}

private fun formulaText(answers: OnboardingAnswers, projection: Projection): String {
    val age = answers.ageBand?.midAge ?: DEFAULT_AGE
    val source = if (answers.isMeasured) "measured app time" else "your daily estimate"
    return listOf(
        "Short-form a day = $source × Reels/Shorts share (${answers.shortFormShare.label.lowercase()}) = ${formatMinutes(projection.dailyShortFormMinutes)}",
        "Days a year = daily minutes × 365 ÷ 60 ÷ 24 ≈ ${projection.yearlyDays}",
        "Waking years = (minutes ÷ 60 ÷ 16 waking hours) × (78 − age $age) = ${oneDecimal(projection.lifetimeYears)}",
        "Time back = daily minutes × 7 × ${(RECLAIM_FACTOR * 100).roundToInt()}% ÷ 60 (conservative; the one sec study measured 57% fewer opens)"
    ).joinToString("\n\n")
}

@Composable
private fun RevealLine(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(PresentMotion.EnterMs)) + slideInVertically(PresentMotion.gentleSpring()) { it / 3 }) {
        Column { content() }
    }
}

@Composable
fun UpsideStep(projection: Projection, onContinue: () -> Unit) {
    val revealed = rememberRevealed(count = 3, stepMs = 900)
    val today = projection.dailyShortFormMinutes
    val withPresent = (today * (1 - RECLAIM_FACTOR)).roundToInt()
    StepLayout(footer = {
        AnimatedVisibility(visible = revealed >= 3, enter = fadeIn() + slideInVertically { it / 2 }) {
            PrimaryButton("Show me my plan", onContinue)
        }
    }) {
        Spacer(modifier = Modifier.height(16.dp))
        StepTitle("Now the good part.", "Even a conservative cut gives you a lot back.")
        Spacer(modifier = Modifier.height(28.dp))
        Bar(label = "Today", minutes = today, fraction = 1f, brush = Brush.horizontalGradient(listOf(PresentPalette.Coral, PresentPalette.Sunrise)), delayMs = 0)
        Spacer(modifier = Modifier.height(16.dp))
        Bar(
            label = "With Present",
            minutes = withPresent,
            fraction = (1 - RECLAIM_FACTOR).toFloat(),
            brush = Brush.horizontalGradient(listOf(PresentPalette.Mint, PresentPalette.MintSoft)),
            delayMs = 500
        )
        Spacer(modifier = Modifier.height(28.dp))
        RevealLine(visible = revealed >= 1) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("~", style = PresentType.Display, color = PresentPalette.Mint)
                CountUpText(
                    target = projection.reclaimedWeeklyHours,
                    format = { "${oneDecimal(it)} h" },
                    style = PresentType.Display,
                    color = PresentPalette.Mint
                )
            }
            Text("back every week", style = PresentType.Title, color = PresentPalette.OnNight)
        }
        Spacer(modifier = Modifier.height(20.dp))
        projection.equivalents.take(2).forEachIndexed { index, equivalent ->
            RevealLine(visible = revealed >= 2 + index) {
                NightCardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(equivalent.emoji, style = PresentType.Headline)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(equivalent.text, style = PresentType.Title, color = PresentPalette.OnNight)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun Bar(label: String, minutes: Int, fraction: Float, brush: Brush, delayMs: Int) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        grow.animateTo(fraction, tween(900, easing = FastOutSlowInEasing))
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = PresentType.Label, color = PresentPalette.OnNightMuted, modifier = Modifier.weight(1f))
            Text("${formatMinutes(minutes)} / day", style = PresentType.Label, color = PresentPalette.OnNight)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(PresentShapes.Pill)
                .background(PresentPalette.NightCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(grow.value.coerceIn(0.001f, 1f))
                    .fillMaxHeight()
                    .clip(PresentShapes.Pill)
                    .background(brush)
            )
        }
    }
}
