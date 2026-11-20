package com.mindguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.ui.components.NightSurface
import com.mindguard.ui.components.PresentCard
import com.mindguard.ui.components.PresentScreen
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.components.SunrisePillButton
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentType

@Composable
fun PermissionsScreen(
    isGranted: Boolean,
    needsRestrictedSettingsStep: Boolean,
    onOpenAppInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit
) {
    LaunchedEffect(isGranted) { if (isGranted) onContinue() }

    PresentScreen(
        contentPadding = PaddingValues(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NightSurface(
            shape = NightHeaderShape,
            contentPadding = 28.dp,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(PresentPalette.NightCard),
                contentAlignment = Alignment.Center
            ) {
                Text("🔐", style = PresentType.Display.copy(fontSize = PresentType.Headline.fontSize * 1.4f))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "One permission to go",
                style = PresentType.Headline,
                color = PresentPalette.OnNight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Present uses Android's Accessibility API to detect short-video feeds (Reels, Shorts, Spotlight) — entirely on-device. No data leaves your phone.",
                style = PresentType.Body,
                color = PresentPalette.OnNightMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(24.dp))

            if (needsRestrictedSettingsStep) {
                Staggered(index = 0) {
                    StepCard(
                        number = "1",
                        title = "Allow Restricted Settings",
                        description = "Android 13+ requires this extra step for apps installed outside the Play Store.",
                        steps = listOf(
                            "Tap \"Open App Info\" below",
                            "Tap the ⋮ menu in the top-right corner",
                            "Tap \"Allow restricted settings\"",
                            "Come back here when done"
                        ),
                        buttonLabel = "Open App Info",
                        onClick = onOpenAppInfo
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Staggered(index = 1) {
                    StepCard(
                        number = "2",
                        title = "Enable Accessibility Access",
                        description = "After step 1 is done, open Accessibility Settings and enable Present.",
                        steps = listOf(
                            "Find \"Present\" in the list",
                            "Tap it and enable the toggle"
                        ),
                        buttonLabel = "Open Accessibility Settings",
                        onClick = onOpenSettings
                    )
                }
            } else {
                Staggered(index = 0) {
                    StepCard(
                        number = "1",
                        title = "Enable Accessibility Access",
                        description = "Find \"Present\" in the list, tap it, enable the toggle.",
                        steps = emptyList(),
                        buttonLabel = "Open Accessibility Settings",
                        onClick = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    number: String,
    title: String,
    description: String,
    steps: List<String>,
    buttonLabel: String,
    onClick: () -> Unit
) {
    PresentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PresentPalette.sunriseGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(number, style = PresentType.Label, color = PresentPalette.CardLight)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = PresentType.Title, color = PresentPalette.Ink)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(description, style = PresentType.Body, color = PresentPalette.InkMuted)

        if (steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            steps.forEachIndexed { i, step ->
                Text(
                    "${i + 1}. $step",
                    style = PresentType.Label,
                    color = PresentPalette.Ink,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SunrisePillButton(
            text = buttonLabel,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            height = 50.dp
        )
    }
}
