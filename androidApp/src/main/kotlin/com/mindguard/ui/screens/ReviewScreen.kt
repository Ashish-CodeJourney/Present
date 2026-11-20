package com.mindguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindguard.shared.policy.PolicyRule
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.ui.components.ChipTone
import com.mindguard.ui.components.EmojiBadge
import com.mindguard.ui.components.NightSurface
import com.mindguard.ui.components.OutlinePillButton
import com.mindguard.ui.components.PresentCard
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.components.SunrisePillButton
import com.mindguard.ui.components.SurfaceChip
import com.mindguard.ui.present.allowedSurfaceLabel
import com.mindguard.ui.present.blockedSurfaceLabel
import com.mindguard.ui.present.platformEmoji
import com.mindguard.ui.present.platformName
import com.mindguard.ui.present.scheduleLabel
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentType

private const val CHIP_BASE_DELAY_MS = 220

@Composable
fun ReviewScreen(
    policy: PresentPolicy,
    onActivate: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PresentPalette.Cream)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Staggered(index = 0) {
                Column {
                    Text("Review your policy", style = PresentType.Headline, color = PresentPalette.Ink)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Nothing changes until you tap Activate.",
                        style = PresentType.Body,
                        color = PresentPalette.InkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Staggered(index = 1) {
                NightSurface {
                    Text("✨ Gemini understood", style = PresentType.Label, color = PresentPalette.SunriseSoft)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(policy.summary, style = PresentType.Title, color = PresentPalette.OnNight)
                    Spacer(modifier = Modifier.height(14.dp))
                    SurfaceChip(
                        text = "${policy.rules.size} ${if (policy.rules.size == 1) "rule" else "rules"}",
                        tone = ChipTone.Night,
                        appearDelayMs = CHIP_BASE_DELAY_MS
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (policy.rules.isEmpty()) {
                Text(
                    "This policy has no rules yet. Tap Edit to describe what to block.",
                    style = PresentType.Body,
                    color = PresentPalette.InkMuted
                )
            }

            policy.rules.forEachIndexed { index, rule ->
                Staggered(index = index + 2) {
                    RuleCard(rule = rule, chipDelayMs = CHIP_BASE_DELAY_MS + index * PresentMotion.StaggerMs * 3)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        SunrisePillButton(
            text = "Activate",
            onClick = onActivate,
            enabled = policy.rules.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            height = 60.dp
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinePillButton(
            text = "Edit",
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            height = 48.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleCard(rule: PolicyRule, chipDelayMs: Int) {
    PresentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(emoji = platformEmoji(rule.platform), background = PresentPalette.Cream)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(platformName(rule.platform), style = PresentType.Title, color = PresentPalette.Ink)
                Text("🕘 ${scheduleLabel(rule.schedule)}", style = PresentType.Label, color = PresentPalette.InkMuted)
            }
        }

        if (rule.targetSurfaces.isNotEmpty() || rule.allowedSurfaces.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rule.targetSurfaces.forEachIndexed { i, surface ->
                    SurfaceChip(
                        text = "Blocked: ${blockedSurfaceLabel(surface)}",
                        tone = ChipTone.Blocked,
                        appearDelayMs = chipDelayMs + i * PresentMotion.StaggerMs,
                        tag = if (surface.isEnforcedOnAndroid) null else "web only"
                    )
                }
                rule.allowedSurfaces.forEachIndexed { i, surface ->
                    SurfaceChip(
                        text = "Allowed: ${allowedSurfaceLabel(surface)}",
                        tone = ChipTone.Allowed,
                        appearDelayMs = chipDelayMs + (rule.targetSurfaces.size + i) * PresentMotion.StaggerMs
                    )
                }
            }
        }

        if (rule.whitelistedEntities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "⭐ Always allowed: ${rule.whitelistedEntities.joinToString(", ")}",
                style = PresentType.Label,
                color = PresentPalette.Ink
            )
        }
    }
}
