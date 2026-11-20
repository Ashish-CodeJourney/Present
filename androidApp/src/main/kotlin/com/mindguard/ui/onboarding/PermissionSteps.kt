package com.mindguard.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.ui.present.SocialQuest
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay

private data class GuideStep(val title: String, val detail: String, val action: String?, val onAction: (() -> Unit)?)

@Composable
fun PermissionStep(
    granted: Boolean,
    needsRestrictedSettingsStep: Boolean,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onContinue: () -> Unit,
    onLater: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(granted) {
        if (granted) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val steps = listOfNotNull(
        if (needsRestrictedSettingsStep) GuideStep(
            "Allow restricted settings",
            "App info → ⋮ (top right) → Allow restricted settings. Android asks this for apps installed outside the Play Store.",
            "Open App info",
            onOpenAppInfo
        ) else null,
        GuideStep(
            "Open Accessibility",
            "Tap Installed apps (or Downloaded apps) → Present.",
            "Open Accessibility settings",
            onOpenAccessibilitySettings
        ),
        GuideStep("Turn Present on", "Switch it on and tap Allow. Then come back here.", null, null)
    )
    StepLayout(footer = {
        if (granted) PrimaryButton("Continue", onContinue)
        else GhostButton("I'll do this later", onLater)
    }) {
        StepTitle("One last step", "Present needs Accessibility access to see when a Reel or Short opens.")
        Spacer(modifier = Modifier.height(20.dp))
        DisclosureCard()
        Spacer(modifier = Modifier.height(20.dp))
        steps.forEachIndexed { index, step ->
            GuideRow(number = index + 1, step = step, done = granted)
            Spacer(modifier = Modifier.height(12.dp))
        }
        AnimatedVisibility(
            visible = granted,
            enter = slideInHorizontally(PresentMotion.gentleSpring()) { it } + fadeIn()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PresentShapes.Card)
                    .background(PresentPalette.Mint)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓", style = PresentType.Headline, color = PresentPalette.Ink)
                Spacer(modifier = Modifier.width(14.dp))
                Text("Present is on. Nice.", style = PresentType.Title, color = PresentPalette.Ink)
            }
        }
    }
}

@Composable
private fun DisclosureCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PresentShapes.Card)
            .background(PresentPalette.CardLight)
            .padding(20.dp)
    ) {
        Text("🔒 Your privacy", style = PresentType.Title, color = PresentPalette.Ink)
        Spacer(modifier = Modifier.height(12.dp))
        Text("What Present reads", style = PresentType.Label, color = PresentPalette.InkMuted)
        Text(
            "The screen's layout and on-screen labels, only inside Instagram, YouTube, Snapchat and TikTok, so it can recognise and close Reels and Shorts. Everything else on your phone is ignored.",
            style = PresentType.Body,
            color = PresentPalette.Ink
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("What it never does", style = PresentType.Label, color = PresentPalette.InkMuted)
        Text(
            "Store or send what's on your screen. It's checked in memory and forgotten instantly: no screenshots, no recordings. Only the sentence you type to Gemini leaves your phone.",
            style = PresentType.Body,
            color = PresentPalette.Ink
        )
    }
}

@Composable
private fun GuideRow(number: Int, step: GuideStep, done: Boolean) {
    NightCardBox {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (done) PresentPalette.Mint else PresentPalette.Sunrise.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (done) "✓" else "$number",
                    style = PresentType.Label,
                    color = if (done) PresentPalette.Ink else PresentPalette.Sunrise
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(step.title, style = PresentType.Title, color = PresentPalette.OnNight)
                Spacer(modifier = Modifier.height(4.dp))
                Text(step.detail, style = PresentType.Body, color = PresentPalette.OnNightMuted)
                if (step.action != null && step.onAction != null && !done) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "${step.action} →",
                        style = PresentType.Title,
                        color = PresentPalette.Sunrise,
                        modifier = Modifier
                            .clip(PresentShapes.Pill)
                            .clickable(onClick = step.onAction)
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DoneStep(
    protected: Boolean,
    quest: SocialQuest,
    canTryReels: Boolean,
    onTryReels: () -> Unit,
    onFinish: () -> Unit
) {
    val glow by rememberInfiniteTransition(label = "shield").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "shield-glow"
    )
    val flip = remember { Animatable(90f) }
    LaunchedEffect(Unit) {
        delay(500)
        flip.animateTo(0f, PresentMotion.gentleSpring())
    }
    StepLayout(footer = {
        PrimaryButton("Go to Present", onFinish)
        if (canTryReels && protected) GhostButton("Try it: open Reels", onTryReels)
    }) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            val tint = if (protected) PresentPalette.Mint else PresentPalette.Sunrise
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(tint.copy(alpha = glow), Color.Transparent)),
                    radius = size.minDimension / 2f
                )
            }
            Text("🛡️", style = PresentType.Display)
        }
        Text(
            if (protected) "You're protected" else "Almost there",
            style = PresentType.Headline,
            color = PresentPalette.OnNight,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (protected) "Reels and Shorts are gone. Your DMs are still there."
            else "Turn on Accessibility from Home whenever you're ready. Your plan is saved.",
            style = PresentType.Body,
            color = PresentPalette.OnNightMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(28.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = flip.value
                    cameraDistance = 12f * density
                    alpha = if (flip.value > 85f) 0f else 1f
                }
                .clip(PresentShapes.Card)
                .background(PresentPalette.sunriseGradient)
                .padding(20.dp)
        ) {
            Text("YOUR FIRST REAL-WORLD QUEST", style = PresentType.Label, color = PresentPalette.Ink.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("${quest.emoji} ${quest.title}", style = PresentType.Title, color = PresentPalette.Ink)
            Spacer(modifier = Modifier.height(4.dp))
            Text(quest.detail, style = PresentType.Body, color = PresentPalette.Ink)
        }
    }
}
