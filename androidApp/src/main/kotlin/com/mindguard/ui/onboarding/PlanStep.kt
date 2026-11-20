package com.mindguard.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.ui.present.CompileUiState
import com.mindguard.ui.present.allowedSurfaceLabel
import com.mindguard.ui.present.blockedSurfaceLabel
import com.mindguard.ui.present.platformEmoji
import com.mindguard.ui.present.platformName
import com.mindguard.ui.present.scheduleLabel
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay

private const val TYPE_CHAR_MS = 22L
private const val COMMIT_CELEBRATION_MS = 1500L

@Composable
fun PlanStep(
    intent: String,
    compileState: CompileUiState,
    onCompile: (String) -> Unit,
    onActivate: (PresentPolicy) -> Unit,
    onEdit: () -> Unit,
    onSkip: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(true) }
    LaunchedEffect(intent) {
        intent.indices.forEach { end ->
            text = intent.substring(0, end + 1)
            delay(TYPE_CHAR_MS)
        }
        typing = false
    }
    StepLayout(footer = { PlanFooter(compileState, typing, text, onCompile, onActivate, onEdit, onSkip) }) {
        StepTitle("Your plan", "Written from your answers. Edit anything, then Gemini turns it into rules.")
        Spacer(modifier = Modifier.height(20.dp))
        if (typing) {
            NightCardBox {
                Text("$text▍", style = PresentType.Title, color = PresentPalette.OnNight)
            }
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                enabled = compileState !is CompileUiState.Loading && compileState !is CompileUiState.Review,
                textStyle = PresentType.Title,
                shape = PresentShapes.Card,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PresentPalette.OnNight,
                    unfocusedTextColor = PresentPalette.OnNight,
                    disabledTextColor = PresentPalette.OnNightMuted,
                    focusedContainerColor = PresentPalette.NightCard,
                    unfocusedContainerColor = PresentPalette.NightCard,
                    disabledContainerColor = PresentPalette.NightCard,
                    focusedBorderColor = PresentPalette.Sunrise,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    cursorColor = PresentPalette.Sunrise
                )
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        when (compileState) {
            CompileUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = PresentPalette.Sunrise, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Gemini is reading your plan…", style = PresentType.Body, color = PresentPalette.OnNightMuted)
            }
            is CompileUiState.Review -> CompiledPolicy(compileState.policy)
            is CompileUiState.Rejected -> ProblemCard("Gemini couldn't use that", compileState.reason)
            is CompileUiState.Error -> ProblemCard("Couldn't reach Gemini", compileState.message)
            CompileUiState.Idle -> Unit
        }
    }
}

@Composable
private fun PlanFooter(
    compileState: CompileUiState,
    typing: Boolean,
    text: String,
    onCompile: (String) -> Unit,
    onActivate: (PresentPolicy) -> Unit,
    onEdit: () -> Unit,
    onSkip: () -> Unit
) {
    when (compileState) {
        is CompileUiState.Review -> {
            PrimaryButton("Activate", { onActivate(compileState.policy) })
            GhostButton("Edit the sentence", onEdit)
        }
        is CompileUiState.Rejected, is CompileUiState.Error -> {
            PrimaryButton("Try again", { onCompile(text) }, enabled = text.isNotBlank())
            GhostButton("Skip for now", onSkip)
        }
        else -> {
            PrimaryButton(
                "Compile with Gemini ✨",
                { onCompile(text) },
                enabled = !typing && text.isNotBlank() && compileState != CompileUiState.Loading
            )
            GhostButton("Skip for now", onSkip)
        }
    }
}

@Composable
private fun ProblemCard(title: String, message: String) {
    NightCardBox {
        Column {
            Text(title, style = PresentType.Title, color = PresentPalette.Coral)
            Spacer(modifier = Modifier.height(6.dp))
            Text(message, style = PresentType.Body, color = PresentPalette.OnNightMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "You can try again, or skip and set this up later from Home.",
                style = PresentType.Label,
                color = PresentPalette.OnNightMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompiledPolicy(policy: PresentPolicy) {
    Column {
        Text("✨ Gemini understood", style = PresentType.Label, color = PresentPalette.Sunrise)
        Spacer(modifier = Modifier.height(4.dp))
        Text(policy.summary, style = PresentType.Title, color = PresentPalette.OnNight)
        Spacer(modifier = Modifier.height(16.dp))
        var chipIndex = 0
        policy.rules.forEach { rule ->
            NightCardBox {
                Column {
                    Text(
                        "${platformEmoji(rule.platform)} ${platformName(rule.platform)} · ${scheduleLabel(rule.schedule)}",
                        style = PresentType.Title,
                        color = PresentPalette.OnNight
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rule.allowedSurfaces.forEach { surface ->
                            PolicyChip("✓ ${allowedSurfaceLabel(surface)}", PresentPalette.Mint, chipIndex++)
                        }
                        rule.targetSurfaces.forEach { surface ->
                            PolicyChip("✕ ${blockedSurfaceLabel(surface)}", PresentPalette.Coral, chipIndex++)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PolicyChip(label: String, color: Color, index: Int) {
    val pop = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        delay(120L + index * 2L * PresentMotion.StaggerMs)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 500f))
    }
    Text(
        label,
        style = PresentType.Label,
        color = PresentPalette.Ink,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .clip(PresentShapes.Pill)
            .background(color)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun CommitStep(onCommitted: () -> Unit) {
    var committed by remember { mutableStateOf(false) }
    LaunchedEffect(committed) {
        if (!committed) return@LaunchedEffect
        delay(COMMIT_CELEBRATION_MS)
        onCommitted()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        StepLayout {
            Spacer(modifier = Modifier.height(24.dp))
            StepTitle("Make it a promise", "To yourself, not to us. Press and hold.")
            Spacer(modifier = Modifier.height(48.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                HoldToCommitButton(text = "I choose to be present", onCommitted = { committed = true })
            }
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedVisibility(visible = committed, enter = fadeIn() + slideInVertically { it / 2 }) {
                Text(
                    "Promise made. 🌅",
                    style = PresentType.Headline,
                    color = PresentPalette.Sunrise,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Confetti(play = committed)
    }
}
