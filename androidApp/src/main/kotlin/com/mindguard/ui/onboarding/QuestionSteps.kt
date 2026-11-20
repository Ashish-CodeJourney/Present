package com.mindguard.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.onboarding.AgeBand
import com.mindguard.onboarding.HardestTime
import com.mindguard.onboarding.KeepSurface
import com.mindguard.onboarding.Motivation
import com.mindguard.onboarding.OnboardingAnswers
import com.mindguard.onboarding.ShortFormShare
import com.mindguard.onboarding.Struggle
import com.mindguard.onboarding.TrackedApp
import com.mindguard.onboarding.formatMinutes
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay

private const val SPLASH_MS = 950L
private const val CARD_ADVANCE_MS = 260L

@Composable
fun StepLayout(
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            content = content
        )
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), content = footer)
    }
}

@Composable
fun SplashStep(onDone: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(650))
        delay(SPLASH_MS - 650)
        onDone()
    }
    val pulse by rememberInfiniteTransition(label = "heartbeat").animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "heartbeat-scale"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Present",
                style = PresentType.Display,
                color = PresentPalette.OnNight,
                modifier = Modifier.drawWithContent {
                    clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() }
                }
            )
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, bottom = 14.dp)
                    .size(14.dp)
                    .scale(if (reveal.value >= 1f) pulse else reveal.value)
                    .clip(CircleShape)
                    .background(PresentPalette.Sunrise)
            )
        }
    }
}

@Composable
fun WelcomeStep(onStart: () -> Unit) {
    StepLayout(footer = { PrimaryButton("Show me", onStart) }) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PhoneMock() }
        Spacer(modifier = Modifier.height(28.dp))
        Text("You opened Instagram for one message.", style = PresentType.Headline, color = PresentPalette.OnNight)
        Spacer(modifier = Modifier.height(6.dp))
        Text("40 minutes later…", style = PresentType.Headline, color = PresentPalette.Sunrise)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            "Let's see where your time really goes. 60 seconds.",
            style = PresentType.Body,
            color = PresentPalette.OnNightMuted
        )
    }
}

private val REEL_COLORS = listOf(
    listOf(Color(0xFFFF5C7A), Color(0xFFFF8A3D)),
    listOf(Color(0xFF6A5AE0), Color(0xFF3DDC97)),
    listOf(Color(0xFFFFB463), Color(0xFFFF5C7A)),
    listOf(Color(0xFF302B63), Color(0xFF6A5AE0))
)
private val REEL_EMOJI = listOf("🐶", "🍕", "🚗", "💃")

@Composable
private fun PhoneMock() {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(300)
        phase = 1
        delay(1100)
        phase = 2
    }
    val scroll by rememberInfiniteTransition(label = "reels").animateFloat(
        initialValue = 0f,
        targetValue = REEL_EMOJI.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(REEL_EMOJI.size * 900, easing = LinearEasing)),
        label = "reels-scroll"
    )
    val minutes = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == 2) minutes.animateTo(40f, tween(3600, easing = LinearEasing))
    }
    Box(
        modifier = Modifier
            .width(170.dp)
            .height(300.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF09071C))
            .border(3.dp, PresentPalette.NightCard, RoundedCornerShape(28.dp))
            .padding(8.dp)
    ) {
        if (phase < 2) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text("💬 Dhruvi", style = PresentType.Label, color = PresentPalette.OnNightMuted)
                Spacer(modifier = Modifier.height(10.dp))
                AnimatedVisibility(visible = phase >= 1, enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)) + fadeIn()) {
                    Text(
                        "did you see this? 😂",
                        style = PresentType.Body,
                        color = PresentPalette.Ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(PresentPalette.CardLight)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
                REEL_EMOJI.indices.forEach { i ->
                    val position = ((i - scroll) % REEL_EMOJI.size + REEL_EMOJI.size) % REEL_EMOJI.size
                    val shown = if (position > REEL_EMOJI.size - 1) position - REEL_EMOJI.size else position
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = shown * size.height }
                            .background(Brush.verticalGradient(REEL_COLORS[i])),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(REEL_EMOJI[i], style = PresentType.Display)
                    }
                }
                Text(
                    "⏱ ${minutes.value.toInt()} min",
                    style = PresentType.Label,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(PresentShapes.Pill)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AppsStep(answers: OnboardingAnswers, onChange: (OnboardingAnswers) -> Unit, onContinue: () -> Unit) {
    StepLayout(footer = { PrimaryButton("Continue", onContinue, enabled = canContinue(OnboardingStep.APPS, answers)) }) {
        StepTitle("Which apps pull you in?", "Pick every one that eats your time.")
        Spacer(modifier = Modifier.height(24.dp))
        val apps = TrackedApp.entries
        apps.chunked(2).forEachIndexed { row, pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { column, app ->
                    SelectTile(
                        emoji = app.emoji,
                        label = app.label,
                        selected = app in answers.apps,
                        index = row * 2 + column,
                        onClick = { onChange(answers.copy(apps = answers.apps.toggle(app))) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun DailyTimeStep(
    answers: OnboardingAnswers,
    onChange: (OnboardingAnswers) -> Unit,
    onMeasure: () -> Unit,
    onContinue: () -> Unit
) {
    StepLayout(footer = { PrimaryButton("Continue", onContinue) }) {
        StepTitle("On a normal day, how long are you on them?", "A rough guess is fine.")
        Spacer(modifier = Modifier.height(32.dp))
        EmojiSlider(
            minutes = answers.dailyMinutes,
            onMinutesChange = { onChange(answers.copy(dailyMinutes = it, measuredMinutesByApp = emptyMap())) }
        )
        Spacer(modifier = Modifier.height(28.dp))
        if (answers.isMeasured) {
            Text(
                "Measured ✓ ${formatMinutes(answers.measuredMinutesByApp.values.sum())} a day. We'll use your real numbers.",
                style = PresentType.Body,
                color = PresentPalette.Mint,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                "Not sure? Measure it for me →",
                style = PresentType.Title,
                color = PresentPalette.Sunrise,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PresentShapes.Pill)
                    .clickable(onClick = onMeasure)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun UsageAccessStep(
    granted: Boolean,
    measuring: Boolean,
    answers: OnboardingAnswers,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    StepLayout(footer = {
        if (answers.isMeasured) PrimaryButton("Continue", onContinue)
        else PrimaryButton("Open Usage Access", onOpenSettings, enabled = !granted)
    }) {
        StepTitle("Let your phone tell us", "Present reads how many minutes each app was open over the last 7 days. Nothing leaves your phone.")
        Spacer(modifier = Modifier.height(24.dp))
        NightCardBox {
            Column {
                listOf(
                    "1" to "Tap Open Usage Access",
                    "2" to "Find Present in the list",
                    "3" to "Turn on \"Permit usage access\", then come back"
                ).forEach { (number, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(PresentPalette.Sunrise.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) { Text(number, style = PresentType.Label, color = PresentPalette.Sunrise) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text, style = PresentType.Body, color = PresentPalette.OnNight)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (measuring) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = PresentPalette.Sunrise, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Measuring your last 7 days…", style = PresentType.Body, color = PresentPalette.OnNightMuted)
            }
        }
        AnimatedVisibility(visible = answers.isMeasured, enter = slideInVertically { it / 2 } + fadeIn()) {
            NightCardBox {
                Column {
                    Text(
                        "Measured ✓ ${formatMinutes(answers.measuredMinutesByApp.values.sum())} a day",
                        style = PresentType.Headline,
                        color = PresentPalette.Mint
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    answers.measuredMinutesByApp.entries.sortedByDescending { it.value }.forEach { (app, minutes) ->
                        Text("${app.emoji} ${app.label} · ${formatMinutes(minutes)}", style = PresentType.Body, color = PresentPalette.OnNight)
                    }
                }
            }
        }
        if (granted && !measuring && !answers.isMeasured) {
            Text(
                "We couldn't find usage for these apps yet. Skip and use the slider instead.",
                style = PresentType.Body,
                color = PresentPalette.OnNightMuted
            )
        }
    }
}

@Composable
private fun SingleChoiceStep(
    title: String,
    subtitle: String?,
    options: List<Triple<String, String?, Boolean>>,
    onPick: (Int) -> Unit
) {
    StepLayout {
        StepTitle(title, subtitle)
        Spacer(modifier = Modifier.height(24.dp))
        options.forEachIndexed { index, (label, emoji, selected) ->
            OptionCard(label = label, emoji = emoji, selected = selected, index = index, onClick = { onPick(index) })
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun rememberAdvanceAfterPick(onContinue: () -> Unit): (() -> Unit) -> Unit {
    var pending by remember { mutableStateOf(0) }
    LaunchedEffect(pending) {
        if (pending == 0) return@LaunchedEffect
        delay(CARD_ADVANCE_MS)
        onContinue()
    }
    return { apply ->
        apply()
        pending += 1
    }
}

@Composable
fun ShortFormStep(answers: OnboardingAnswers, onChange: (OnboardingAnswers) -> Unit, onContinue: () -> Unit) {
    val pick = rememberAdvanceAfterPick(onContinue)
    val shares = ShortFormShare.entries
    SingleChoiceStep(
        title = "How much of that is Reels / Shorts?",
        subtitle = "The endless vertical videos.",
        options = shares.map { Triple(it.label, null, it == answers.shortFormShare) },
        onPick = { index -> pick { onChange(answers.copy(shortFormShare = shares[index])) } }
    )
}

@Composable
fun HardestTimeStep(answers: OnboardingAnswers, onChange: (OnboardingAnswers) -> Unit, onContinue: () -> Unit) {
    val pick = rememberAdvanceAfterPick(onContinue)
    val times = HardestTime.entries
    SingleChoiceStep(
        title = "When does it hit hardest?",
        subtitle = "We'll protect this time first.",
        options = times.map { Triple(it.label, it.emoji, it == answers.hardestTime) },
        onPick = { index -> pick { onChange(answers.copy(hardestTime = times[index])) } }
    )
}

@Composable
fun StrugglesStep(answers: OnboardingAnswers, onChange: (OnboardingAnswers) -> Unit, onContinue: () -> Unit) {
    StepLayout(footer = { PrimaryButton("Continue", onContinue) }) {
        StepTitle("What usually happens?", "Pick any that feel familiar. This happens to almost everyone.")
        Spacer(modifier = Modifier.height(24.dp))
        Struggle.entries.forEachIndexed { index, struggle ->
            OptionCard(
                label = struggle.label,
                selected = struggle in answers.struggles,
                index = index,
                onClick = { onChange(answers.copy(struggles = answers.struggles.toggle(struggle))) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun MotivationsStep(answers: OnboardingAnswers, onChange: (OnboardingAnswers) -> Unit, onContinue: () -> Unit) {
    StepLayout(footer = { PrimaryButton("Continue", onContinue) }) {
        StepTitle("What would you do with an extra hour?", "Pick as many as you like.")
        Spacer(modifier = Modifier.height(24.dp))
        Motivation.entries.chunked(2).forEachIndexed { row, pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { column, motivation ->
                    SelectTile(
                        emoji = motivation.emoji,
                        label = motivation.label,
                        selected = motivation in answers.motivations,
                        index = row * 2 + column,
                        onClick = { onChange(answers.copy(motivations = answers.motivations.toggle(motivation))) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeepStep(answers: OnboardingAnswers, onChange: (OnboardingAnswers) -> Unit, onContinue: () -> Unit) {
    StepLayout(footer = { PrimaryButton("Continue", onContinue) }) {
        StepTitle("What must stay usable?", "Present removes Reels and Shorts, not your apps.")
        Spacer(modifier = Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KeepSurface.entries.forEachIndexed { index, surface ->
                KeepChip(
                    label = surface.label,
                    selected = surface in answers.keep,
                    index = index,
                    onClick = { onChange(answers.copy(keep = answers.keep.toggle(surface))) }
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        NightCardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", style = PresentType.Headline)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("We never block these", style = PresentType.Title, color = PresentPalette.Mint)
                    Text(
                        "Reply to friends, look things up, watch the channels you chose. Only the endless feed goes.",
                        style = PresentType.Body,
                        color = PresentPalette.OnNightMuted
                    )
                }
            }
        }
    }
}

@Composable
fun AgeStep(answers: OnboardingAnswers, onChange: (OnboardingAnswers) -> Unit, onContinue: () -> Unit) {
    val pick = rememberAdvanceAfterPick(onContinue)
    val bands = AgeBand.entries
    SingleChoiceStep(
        title = "How old are you?",
        subtitle = "Optional. It makes the lifetime estimate honest.",
        options = bands.map { Triple(it.label, null, it == answers.ageBand) },
        onPick = { index -> pick { onChange(answers.copy(ageBand = bands[index])) } }
    )
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
