package com.mindguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.ui.components.ChipTone
import com.mindguard.ui.components.EmojiBadge
import com.mindguard.ui.components.NightSurface
import com.mindguard.ui.components.OutlinePillButton
import com.mindguard.ui.components.PresentCard
import com.mindguard.ui.components.PresentScreen
import com.mindguard.ui.components.ProgressRing
import com.mindguard.ui.components.SectionLabel
import com.mindguard.ui.components.SolidPillButton
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.components.SunrisePillButton
import com.mindguard.ui.components.SurfaceChip
import com.mindguard.ui.components.presentTextFieldColors
import com.mindguard.ui.present.QuestAction
import com.mindguard.ui.present.SocialQuest
import com.mindguard.ui.present.canConfirmTurnOff
import com.mindguard.ui.present.socialQuestAt
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun TurnOffGuardScreen(
    countdownSeconds: Int,
    turnOffsToday: Long,
    questsAccepted: Long,
    onKeepOn: () -> Unit,
    onStartQuest: (SocialQuest) -> Unit,
    onPauseInstead: () -> Unit,
    onTurnOff: () -> Unit
) {
    var remaining by remember { mutableIntStateOf(countdownSeconds) }
    var questIndex by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) }
    var wantsToLeave by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    val quest = socialQuestAt(questIndex)
    val countdownDone = remaining == 0

    LaunchedEffect(countdownSeconds) {
        remaining = countdownSeconds
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (countdownSeconds > 0) remaining.toFloat() / countdownSeconds.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "guardCountdown"
    )

    PresentScreen(
        contentPadding = PaddingValues(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NightSurface(
            shape = NightHeaderShape,
            contentPadding = 28.dp,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            ProgressRing(progress = progress, size = 132.dp, strokeWidth = 10.dp) {
                Crossfade(
                    targetState = if (countdownDone) "🌅" else "$remaining",
                    animationSpec = tween(durationMillis = 250),
                    label = "guardCountdownText"
                ) { value ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, style = PresentType.Headline, color = PresentPalette.OnNight)
                        if (value != "🌅") {
                            Text("seconds", style = PresentType.Label, color = PresentPalette.OnNightMuted)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Before you switch off Present…",
                style = PresentType.Headline,
                color = PresentPalette.OnNight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (turnOffsToday > 0) "This is turn-off #${turnOffsToday + 1} today. The feed will still be there in an hour. Will the people around you?"
                else "The feed will still be there in an hour. Will the people around you?",
                style = PresentType.Body,
                color = PresentPalette.OnNightMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Staggered(index = 0) {
                QuestCard(
                    quest = quest,
                    onShuffle = { questIndex += 1 },
                    onStart = {
                        onStartQuest(quest)
                    }
                )
            }

            if (questsAccepted > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                SurfaceChip(
                    text = "🌱 You've chosen real life over the feed $questsAccepted ${if (questsAccepted == 1L) "time" else "times"}.",
                    tone = ChipTone.Allowed,
                    appearDelayMs = 250
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Staggered(index = 1) {
                SunrisePillButton(
                    text = "I'll do it. Keep Present on",
                    onClick = onKeepOn,
                    modifier = Modifier.fillMaxWidth(),
                    height = 58.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                !countdownDone -> Text(
                    "You can switch off in ${remaining}s",
                    style = PresentType.Label,
                    color = PresentPalette.InkMuted
                )
                !wantsToLeave -> TextButton(onClick = { wantsToLeave = true }) {
                    Text("I still want to switch off", style = PresentType.Body, color = PresentPalette.InkMuted)
                }
                else -> Unit
            }

            AnimatedVisibility(
                visible = countdownDone && wantsToLeave,
                enter = fadeIn() + expandVertically()
            ) {
                LeaveSection(
                    reason = reason,
                    onReasonChange = { reason = it },
                    onPauseInstead = onPauseInstead,
                    onTurnOff = onTurnOff
                )
            }
        }
    }
}

@Composable
private fun QuestCard(quest: SocialQuest, onShuffle: () -> Unit, onStart: () -> Unit) {
    PresentCard {
        SectionLabel("Your real-world quest", color = PresentPalette.Sunrise)
        Crossfade(targetState = quest, label = "questSwap") { shown ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(emoji = shown.emoji, size = 56.dp, style = PresentType.Headline)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(shown.title, style = PresentType.Title, color = PresentPalette.Ink, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(shown.detail, style = PresentType.Body, color = PresentPalette.InkMuted)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (quest.action != QuestAction.NONE) {
                SolidPillButton(
                    text = if (quest.action == QuestAction.DIAL) "📞 Call now" else "💬 Message now",
                    onClick = onStart,
                    container = PresentPalette.Mint,
                    contentColor = PresentPalette.Ink,
                    height = 46.dp,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinePillButton(
                text = "🔀 Another",
                onClick = onShuffle,
                height = 46.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LeaveSection(
    reason: String,
    onReasonChange: (String) -> Unit,
    onPauseInstead: () -> Unit,
    onTurnOff: () -> Unit
) {
    val canLeave = canConfirmTurnOff(reason)
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What do you need the feed for?") },
            placeholder = { Text("e.g. Posting my college fest Reel") },
            supportingText = { Text("Write a full sentence. Naming it helps you notice it.") },
            textStyle = PresentType.Body,
            shape = PresentShapes.Card,
            colors = presentTextFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        SolidPillButton(
            text = "Pause for 15 minutes instead",
            onClick = onPauseInstead,
            enabled = canLeave,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onTurnOff,
            enabled = canLeave,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                "Turn off completely",
                style = PresentType.Body,
                color = if (canLeave) ChipTone.Blocked.foreground else PresentPalette.InkMuted.copy(alpha = 0.5f)
            )
        }
    }
}
