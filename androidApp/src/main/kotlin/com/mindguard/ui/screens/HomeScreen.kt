package com.mindguard.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.ui.components.ChipTone
import com.mindguard.ui.components.EmojiBadge
import com.mindguard.ui.components.NightSurface
import com.mindguard.ui.components.OutlinePillButton
import com.mindguard.ui.components.PresentCard
import com.mindguard.ui.components.PresentScreen
import com.mindguard.ui.components.ProgressRing
import com.mindguard.ui.components.PulseDot
import com.mindguard.ui.components.SectionLabel
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.components.SunrisePillButton
import com.mindguard.ui.components.SurfaceChip
import com.mindguard.ui.components.greetingForHour
import com.mindguard.ui.components.reclaimedRingProgress
import com.mindguard.ui.components.rememberCountUp
import com.mindguard.ui.present.formatTimeReclaimed
import com.mindguard.ui.present.socialQuestAt
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentType
import java.util.Calendar

private fun formatPauseTime(pauseUntilMs: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = pauseUntilMs
    return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

private enum class GuardStatus(val label: String, val dot: Color) {
    Active("Protected", PresentPalette.Mint),
    Paused("Paused", PresentPalette.Sunrise),
    Off("Protection off", PresentPalette.Coral)
}

@Composable
fun HomeScreen(
    protectionEnabled: Boolean,
    blockCount: Long,
    attemptCount: Long,
    currentStreak: Long,
    pauseUntilMs: Long = 0L,
    onToggleProtection: (Boolean) -> Unit,
    onResumeFromPause: () -> Unit = {},
    onViewStats: () -> Unit,
    onOpenSettings: () -> Unit,
    activePolicySummary: String? = null,
    onEditIntent: () -> Unit = {},
    onNeedFiveMinutes: () -> Unit = {}
) {
    val deflectedPct = if (attemptCount > 0) (blockCount * 100 / attemptCount) else 0L
    val isPaused = pauseUntilMs > 0L && System.currentTimeMillis() < pauseUntilMs
    val status = when {
        isPaused          -> GuardStatus.Paused
        protectionEnabled -> GuardStatus.Active
        else              -> GuardStatus.Off
    }
    val calendar = remember { Calendar.getInstance() }
    val greeting = greetingForHour(calendar.get(Calendar.HOUR_OF_DAY))
    val quest = socialQuestAt(calendar.get(Calendar.DAY_OF_YEAR))

    PresentScreen(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp)) {
        Staggered(index = 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(greeting, style = PresentType.Label, color = PresentPalette.InkMuted)
                    Text("Present", style = PresentType.Headline, color = PresentPalette.Ink)
                }
                if (currentStreak > 0) {
                    SurfaceChip(text = "🔥 $currentStreak day streak", tone = ChipTone.Sunrise, appearDelayMs = 300)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PresentPalette.CardLight)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PresentPalette.Ink)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Staggered(index = 1) {
            HeroCard(
                status = status,
                protectionEnabled = protectionEnabled,
                isPaused = isPaused,
                pauseUntilMs = pauseUntilMs,
                blockCount = blockCount,
                attemptCount = attemptCount,
                deflectedPct = deflectedPct,
                onToggleProtection = onToggleProtection,
                onResumeFromPause = onResumeFromPause
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 2) {
            IntentCard(summary = activePolicySummary, onEditIntent = onEditIntent)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 3) {
            PresentCard {
                SectionLabel("Today's quest", color = PresentPalette.Sunrise)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(emoji = quest.emoji, size = 52.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(quest.title, style = PresentType.Title, color = PresentPalette.Ink)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(quest.detail, style = PresentType.Body, color = PresentPalette.InkMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isPaused) {
            Staggered(index = 4) {
                OutlinePillButton(
                    text = "⏳  I need 5 minutes",
                    onClick = onNeedFiveMinutes,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Staggered(index = 5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EntryCard("📊", "Your stats", "All-time + Gemini insight", onViewStats, Modifier.weight(1f))
                EntryCard("⚙️", "Settings", "Apps & focus hours", onOpenSettings, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroCard(
    status: GuardStatus,
    protectionEnabled: Boolean,
    isPaused: Boolean,
    pauseUntilMs: Long,
    blockCount: Long,
    attemptCount: Long,
    deflectedPct: Long,
    onToggleProtection: (Boolean) -> Unit,
    onResumeFromPause: () -> Unit
) {
    val ringTarget = reclaimedRingProgress(blockCount)
    val ring = remember { Animatable(0f) }
    LaunchedEffect(ringTarget) {
        ring.animateTo(ringTarget, tween(durationMillis = 1_200, easing = FastOutSlowInEasing))
    }
    val shownBlocks = rememberCountUp(blockCount)

    NightSurface(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseDot(color = status.dot)
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(status.label, style = PresentType.Title, color = PresentPalette.OnNight)
                Text(
                    when (status) {
                        GuardStatus.Paused -> "Resumes at ${formatPauseTime(pauseUntilMs)}"
                        GuardStatus.Active -> "Reels & Shorts blocked · DMs kept"
                        GuardStatus.Off    -> "Short-form feeds are open"
                    },
                    style = PresentType.Label,
                    color = PresentPalette.OnNightMuted
                )
            }
            Switch(
                checked = protectionEnabled && !isPaused,
                onCheckedChange = {
                    if (isPaused) onResumeFromPause() else onToggleProtection(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PresentPalette.Mint,
                    checkedBorderColor = PresentPalette.Mint,
                    uncheckedThumbColor = PresentPalette.OnNightMuted,
                    uncheckedTrackColor = PresentPalette.NightCard,
                    uncheckedBorderColor = PresentPalette.OnNightMuted
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        ProgressRing(progress = ring.value, size = 200.dp, strokeWidth = 14.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatTimeReclaimed(shownBlocks),
                    style = PresentType.Headline.copy(fontSize = PresentType.Headline.fontSize * 1.3f),
                    color = PresentPalette.OnNight,
                    textAlign = TextAlign.Center
                )
                Text("reclaimed today", style = PresentType.Label, color = PresentPalette.OnNightMuted)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            HeroStat(value = blockCount.toString(), label = "Blocked", modifier = Modifier.weight(1f))
            HeroStat(value = attemptCount.toString(), label = "Attempts", modifier = Modifier.weight(1f))
            HeroStat(value = "$deflectedPct%", label = "Deflected", modifier = Modifier.weight(1f))
        }

        if (isPaused) {
            Spacer(modifier = Modifier.height(16.dp))
            SunrisePillButton(
                text = "Resume now",
                onClick = onResumeFromPause,
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = PresentType.Title, color = PresentPalette.OnNight)
        Text(label, style = PresentType.Label, color = PresentPalette.OnNightMuted)
    }
}

@Composable
private fun EntryCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PresentCard(modifier = modifier, onClick = onClick, contentPadding = 16.dp) {
        EmojiBadge(emoji = emoji, size = 40.dp, background = PresentPalette.Cream)
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, style = PresentType.Title.copy(fontSize = PresentType.Body.fontSize), color = PresentPalette.Ink)
        Text(subtitle, style = PresentType.Label, color = PresentPalette.InkMuted)
    }
}

@Composable
private fun IntentCard(summary: String?, onEditIntent: () -> Unit) {
    PresentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("🎯 Your intent", color = PresentPalette.Sunrise, modifier = Modifier.weight(1f))
            if (summary != null) {
                OutlinePillButton(
                    text = "Edit",
                    onClick = onEditIntent,
                    height = 36.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (summary != null) {
            Text(summary, style = PresentType.Title, color = PresentPalette.Ink)
        } else {
            Text(
                "Say what you want from your phone, and Gemini turns it into rules.",
                style = PresentType.Body,
                color = PresentPalette.InkMuted
            )
            Spacer(modifier = Modifier.height(14.dp))
            SunrisePillButton(
                text = "Tell Present what you want ✨",
                onClick = onEditIntent,
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp
            )
        }
    }
}
