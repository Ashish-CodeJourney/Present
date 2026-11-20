package com.mindguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.components.SunrisePillButton
import com.mindguard.ui.components.presentTextFieldColors
import com.mindguard.ui.present.canUnlock
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay

private data class SideQuest(val emoji: String, val title: String, val action: String)

private val SIDE_QUESTS = listOf(
    SideQuest("💧", "Hydrate", "Drink a glass of water"),
    SideQuest("👀", "Reset eyes", "Look 20 ft away for 20 s"),
    SideQuest("🧘", "Posture check", "Stand up and stretch")
)

internal val NightHeaderShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)

@Composable
fun PauseCardScreen(
    countdownSeconds: Int,
    onUnlock: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var remaining by remember { mutableIntStateOf(countdownSeconds) }
    var reason by remember { mutableStateOf("") }
    var selectedQuest by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(countdownSeconds) {
        remaining = countdownSeconds
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }

    val countdownDone = remaining == 0
    val progress by animateFloatAsState(
        targetValue = remaining.toFloat() / countdownSeconds.toFloat(),
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "pauseCountdown"
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
            Spacer(modifier = Modifier.height(12.dp))
            ProgressRing(progress = progress, size = 184.dp, strokeWidth = 12.dp) {
                Crossfade(
                    targetState = if (countdownDone) "🌱" else "$remaining",
                    animationSpec = tween(durationMillis = 250),
                    label = "pauseCountdownText"
                ) { value ->
                    Text(value, style = PresentType.Display, color = PresentPalette.OnNight)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Is this what you opened your phone for?",
                style = PresentType.Headline,
                color = PresentPalette.OnNight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = countdownDone, label = "pauseSubtitle") { done ->
                Text(
                    if (done) "If it still matters, tell Present why." else "Take a breath. This pause is the point.",
                    style = PresentType.Body,
                    color = PresentPalette.OnNightMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            AnimatedVisibility(
                visible = countdownDone,
                enter = fadeIn(tween(durationMillis = 400)) + slideInVertically(tween(durationMillis = 400)) { it / 6 }
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionLabel("Or try a side quest instead")

                    SIDE_QUESTS.forEachIndexed { index, quest ->
                        Staggered(index = index) {
                            SideQuestCard(
                                quest = quest,
                                selected = selectedQuest == index,
                                onClick = { selectedQuest = if (selectedQuest == index) null else index }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Why do you need 5 minutes?") },
                        placeholder = { Text("e.g. Replying to a friend's Reel") },
                        textStyle = PresentType.Body,
                        shape = PresentShapes.Card,
                        colors = presentTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinePillButton(
                        text = "Unlock 5 minutes",
                        onClick = { onUnlock(reason.trim()) },
                        enabled = canUnlock(reason),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SunrisePillButton(
                text = "I'm good, take me back",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SideQuestCard(quest: SideQuest, selected: Boolean, onClick: () -> Unit) {
    PresentCard(
        color = if (selected) PresentPalette.MintSoft else PresentPalette.CardLight,
        border = if (selected) BorderStroke(2.dp, PresentPalette.Mint) else null,
        contentPadding = 16.dp,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(
                emoji = quest.emoji,
                background = if (selected) PresentPalette.CardLight else PresentPalette.Cream
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(quest.title, style = PresentType.Title.copy(fontSize = PresentType.Body.fontSize), color = PresentPalette.Ink)
                Text(quest.action, style = PresentType.Label, color = PresentPalette.InkMuted)
            }
            if (selected) {
                Text("✓", style = PresentType.Title, color = ChipTone.Allowed.foreground)
            }
        }
    }
}
