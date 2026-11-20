package com.mindguard.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindguard.ui.present.canUnlock
import kotlinx.coroutines.delay

private data class SideQuest(val emoji: String, val title: String, val action: String)

private val SIDE_QUESTS = listOf(
    SideQuest("💧", "Hydrate", "Drink a glass of water"),
    SideQuest("👀", "Reset eyes", "Look 20 ft away for 20 s"),
    SideQuest("🧘", "Posture check", "Stand up and stretch")
)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PresentColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val inset = Offset(stroke / 2, stroke / 2)
                val ringSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color(0xFFE8E6FF),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = inset,
                    size = ringSize,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = PresentColors.Primary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = inset,
                    size = ringSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                if (countdownDone) "🌱" else "$remaining",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = PresentColors.Ink
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Is this what you opened your phone for?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PresentColors.Ink,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (countdownDone) "If it still matters, tell Present why." else "Take a breath. This pause is the point.",
            fontSize = 14.sp,
            color = PresentColors.Muted,
            textAlign = TextAlign.Center
        )

        if (countdownDone) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Or try a side quest instead",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PresentColors.Muted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            SIDE_QUESTS.forEachIndexed { index, quest ->
                SideQuestCard(
                    quest = quest,
                    selected = selectedQuest == index,
                    onClick = { selectedQuest = if (selectedQuest == index) null else index }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Why do you need 5 minutes?") },
                placeholder = { Text("e.g. Replying to a friend's Reel") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PresentColors.Primary,
                    unfocusedBorderColor = Color(0xFFDAD7F5)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onUnlock(reason.trim()) },
                enabled = canUnlock(reason),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4F00))
            ) {
                Text("Unlock 5 minutes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PresentColors.Primary)
        ) {
            Text("I'm good, take me back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SideQuestCard(quest: SideQuest, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PresentColors.AllowedBg else Color.White
        ),
        border = if (selected) BorderStroke(2.dp, PresentColors.AllowedFg) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(quest.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(quest.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PresentColors.Ink)
                Text(quest.action, fontSize = 13.sp, color = PresentColors.Muted)
            }
            if (selected) {
                Text("✓", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PresentColors.AllowedFg)
            }
        }
    }
}
