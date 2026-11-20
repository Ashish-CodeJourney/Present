package com.mindguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindguard.ui.present.formatTimeReclaimed
import java.util.Calendar

private fun formatPauseTime(pauseUntilMs: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = pauseUntilMs
    return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
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
    val deflectedPct     = if (attemptCount > 0) (blockCount * 100 / attemptCount) else 0L
    val isPaused = pauseUntilMs > 0L && System.currentTimeMillis() < pauseUntilMs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F3FF))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Present", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF635BBB))
            }
        }

        // Streak badge (only if streak > 0)
        if (currentStreak > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFFEDD8)
            ) {
                Text(
                    "🔥 $currentStreak day streak — keep it up!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        IntentCard(summary = activePolicySummary, onEditIntent = onEditIntent)

        Spacer(modifier = Modifier.height(16.dp))

        // Protection card
        val cardColor = when {
            isPaused          -> Color(0xFF7A4F00)
            protectionEnabled -> Color(0xFF1B5E20)
            else              -> Color(0xFF4A1942)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            when {
                                isPaused          -> "⏸️ Paused"
                                protectionEnabled -> "🛡️ Active"
                                else              -> "🔴 Off"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            when {
                                isPaused          -> "Resumes at ${formatPauseTime(pauseUntilMs)}"
                                protectionEnabled -> "Short-form videos are being blocked"
                                else              -> "Protection is off"
                            },
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = protectionEnabled && !isPaused,
                        onCheckedChange = {
                            if (isPaused) onResumeFromPause() else onToggleProtection(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
                if (isPaused) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onResumeFromPause,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Resume Now", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2×2 stats grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("🚫", "Videos Blocked", blockCount.toString(),            Modifier.weight(1f))
            StatCard("⏱️", "Time Reclaimed", formatTimeReclaimed(blockCount), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("👀", "Attempts",   attemptCount.toString(), Modifier.weight(1f))
            StatCard("🎯", "Deflected",  "$deflectedPct%",        Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isPaused) {
            OutlinedButton(
                onClick = onNeedFiveMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("⏳ I need 5 minutes", color = Color(0xFF635BBB), fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        TextButton(onClick = onViewStats) {
            Text("View All-Time Stats →", color = Color(0xFF635BBB), fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
            Text(label, fontSize = 11.sp, color = Color(0xFF888888), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun IntentCard(summary: String?, onEditIntent: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("🎯 Your intent", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF635BBB))
            Spacer(modifier = Modifier.height(6.dp))
            if (summary != null) {
                Text(summary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E), lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onEditIntent, shape = RoundedCornerShape(12.dp)) {
                    Text("Edit", color = Color(0xFF635BBB))
                }
            } else {
                Text(
                    "Say what you want from your phone, and Gemini turns it into rules.",
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onEditIntent,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BBB))
                ) {
                    Text("Tell Present what you want ✨", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
