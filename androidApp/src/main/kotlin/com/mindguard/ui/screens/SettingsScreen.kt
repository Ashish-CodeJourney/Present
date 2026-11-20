package com.mindguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.ui.components.ChipTone
import com.mindguard.ui.components.EmojiBadge
import com.mindguard.ui.components.PresentCard
import com.mindguard.ui.components.PresentScreen
import com.mindguard.ui.components.ScreenTopBar
import com.mindguard.ui.components.SectionLabel
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentType

private val DividerColor = Color(0xFFF3ECE4)

@Composable
fun SettingsScreen(
    instagramEnabled: Boolean,
    youtubeEnabled: Boolean,
    tiktokEnabled: Boolean,
    snapchatEnabled: Boolean,
    focusScheduleEnabled: Boolean,
    focusStartHour: Int,
    focusEndHour: Int,
    currentStreak: Long,
    bestStreak: Long,
    onInstagramToggle: (Boolean) -> Unit,
    onYoutubeToggle: (Boolean) -> Unit,
    onTiktokToggle: (Boolean) -> Unit,
    onSnapchatToggle: (Boolean) -> Unit,
    onFocusScheduleChange: (enabled: Boolean, startHour: Int, endHour: Int) -> Unit,
    onBack: () -> Unit
) {
    var localFocusEnabled by remember(focusScheduleEnabled) { mutableStateOf(focusScheduleEnabled) }
    var localStart by remember(focusStartHour) { mutableIntStateOf(focusStartHour) }
    var localEnd   by remember(focusEndHour)   { mutableIntStateOf(focusEndHour) }

    PresentScreen(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
        ScreenTopBar(title = "Settings", onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 0) {
            Column {
                SectionLabel("🛡️  Blocked apps")
                SettingsCard {
                    AppToggleRow("📸", "Instagram Reels", instagramEnabled, onInstagramToggle)
                    HorizontalDivider(color = DividerColor)
                    AppToggleRow("▶️", "YouTube Shorts", youtubeEnabled, onYoutubeToggle)
                    HorizontalDivider(color = DividerColor)
                    AppToggleRow("🎵", "TikTok", tiktokEnabled, onTiktokToggle)
                    HorizontalDivider(color = DividerColor)
                    AppToggleRow("👻", "Snapchat Spotlight", snapchatEnabled, onSnapchatToggle)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Staggered(index = 1) {
            Column {
                SectionLabel("⏰  Focus schedule")
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-block during work hours", style = PresentType.Body, color = PresentPalette.Ink)
                            Text("Override the manual toggle", style = PresentType.Label, color = PresentPalette.InkMuted)
                        }
                        Switch(
                            checked = localFocusEnabled,
                            onCheckedChange = { enabled ->
                                localFocusEnabled = enabled
                                onFocusScheduleChange(enabled, localStart, localEnd)
                            },
                            colors = presentSwitchColors()
                        )
                    }

                    AnimatedVisibility(
                        visible = localFocusEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider(color = DividerColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HourPicker("From", localStart) { h ->
                                    localStart = h
                                    onFocusScheduleChange(localFocusEnabled, h, localEnd)
                                }
                                Text("→", style = PresentType.Title, color = PresentPalette.InkMuted)
                                HourPicker("To", localEnd) { h ->
                                    localEnd = h
                                    onFocusScheduleChange(localFocusEnabled, localStart, h)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Staggered(index = 2) {
            Column {
                SectionLabel("🔥  Your streak")
                PresentCard(color = PresentPalette.SunriseSoft) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StreakStat("🔥", "Current", "$currentStreak days")
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(PresentPalette.Sunrise.copy(alpha = 0.25f))
                        )
                        StreakStat("🏆", "Best", "$bestStreak days")
                    }
                }
            }
        }
    }
}

@Composable
private fun presentSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = PresentPalette.Sunrise,
    checkedBorderColor = PresentPalette.Sunrise,
    uncheckedThumbColor = PresentPalette.InkMuted,
    uncheckedTrackColor = PresentPalette.Cream,
    uncheckedBorderColor = PresentPalette.InkMuted.copy(alpha = 0.5f)
)

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    PresentCard(contentPadding = 0.dp, content = content)
}

@Composable
private fun AppToggleRow(emoji: String, label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiBadge(emoji = emoji, size = 40.dp, background = PresentPalette.Cream, style = PresentType.Body)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = PresentType.Body, modifier = Modifier.weight(1f), color = PresentPalette.Ink)
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = presentSwitchColors()
        )
    }
}

@Composable
private fun HourPicker(label: String, hour: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = PresentType.Label, color = PresentPalette.InkMuted)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−") { onChange((hour - 1 + 24) % 24) }
            Text(
                "%02d:00".format(hour),
                style = PresentType.Title,
                color = PresentPalette.Ink,
                modifier = Modifier.width(72.dp),
                textAlign = TextAlign.Center
            )
            StepButton("+") { onChange((hour + 1) % 24) }
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(PresentPalette.SunriseSoft)
    ) {
        Text(symbol, style = PresentType.Title, color = ChipTone.Sunrise.foreground)
    }
}

@Composable
private fun StreakStat(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = PresentType.Headline)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = PresentType.Title, color = ChipTone.Sunrise.foreground)
        Text(label, style = PresentType.Label, color = PresentPalette.InkMuted)
    }
}
