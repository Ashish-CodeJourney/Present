package com.mindguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindguard.shared.policy.PolicyRule
import com.mindguard.shared.policy.PresentPolicy
import com.mindguard.ui.present.allowedSurfaceLabel
import com.mindguard.ui.present.blockedSurfaceLabel
import com.mindguard.ui.present.platformEmoji
import com.mindguard.ui.present.platformName
import com.mindguard.ui.present.scheduleLabel

@Composable
fun ReviewScreen(
    policy: PresentPolicy,
    onActivate: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PresentColors.Background)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Review your policy", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PresentColors.Ink)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Nothing changes until you tap Activate.",
                fontSize = 14.sp,
                color = PresentColors.Muted
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PresentColors.Primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("✨ Gemini understood", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(policy.summary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 24.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (policy.rules.isEmpty()) {
                Text(
                    "This policy has no rules yet. Tap Edit to describe what to block.",
                    fontSize = 14.sp,
                    color = PresentColors.Muted
                )
            }

            policy.rules.forEach { rule ->
                RuleCard(rule)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onActivate,
            enabled = policy.rules.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PresentColors.AllowedFg)
        ) {
            Text("Activate", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Edit", color = PresentColors.Primary, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleCard(rule: PolicyRule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(platformEmoji(rule.platform), fontSize = 26.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(platformName(rule.platform), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PresentColors.Ink)
                    Text("🕘 ${scheduleLabel(rule.schedule)}", fontSize = 13.sp, color = PresentColors.Muted)
                }
            }

            if (rule.targetSurfaces.isNotEmpty() || rule.allowedSurfaces.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rule.targetSurfaces.forEach { surface ->
                        PolicyChip(
                            text = "Blocked: ${blockedSurfaceLabel(surface)}",
                            background = PresentColors.BlockedBg,
                            foreground = PresentColors.BlockedFg,
                            webOnly = !surface.isEnforcedOnAndroid
                        )
                    }
                    rule.allowedSurfaces.forEach { surface ->
                        PolicyChip(
                            text = "Allowed: ${allowedSurfaceLabel(surface)}",
                            background = PresentColors.AllowedBg,
                            foreground = PresentColors.AllowedFg,
                            webOnly = false
                        )
                    }
                }
            }

            if (rule.whitelistedEntities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "⭐ Always allowed: ${rule.whitelistedEntities.joinToString(", ")}",
                    fontSize = 13.sp,
                    color = PresentColors.Ink
                )
            }
        }
    }
}

@Composable
private fun PolicyChip(text: String, background: Color, foreground: Color, webOnly: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = foreground)
            if (webOnly) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(shape = RoundedCornerShape(50), color = PresentColors.WebOnlyBg) {
                    Text(
                        "web only",
                        fontSize = 10.sp,
                        color = PresentColors.WebOnlyFg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
