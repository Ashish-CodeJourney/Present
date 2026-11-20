package com.mindguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindguard.ui.present.QuestAction
import com.mindguard.ui.present.SocialQuest
import com.mindguard.ui.present.canConfirmTurnOff
import com.mindguard.ui.present.socialQuestAt
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PresentColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text("✋", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Before you switch off Present…",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PresentColors.Ink,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (turnOffsToday > 0) "This is turn-off #${turnOffsToday + 1} today. The feed will still be there in an hour. Will the people around you?"
            else "The feed will still be there in an hour. Will the people around you?",
            fontSize = 14.sp,
            color = PresentColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        QuestCard(
            quest = quest,
            onShuffle = { questIndex += 1 },
            onStart = {
                onStartQuest(quest)
            }
        )

        if (questsAccepted > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "🌱 You've chosen real life over the feed $questsAccepted ${if (questsAccepted == 1L) "time" else "times"}.",
                fontSize = 13.sp,
                color = PresentColors.AllowedFg,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onKeepOn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PresentColors.Primary)
        ) {
            Text("I'll do it. Keep Present on", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            !countdownDone -> Text(
                "You can switch off in ${remaining}s",
                fontSize = 13.sp,
                color = PresentColors.Muted
            )
            !wantsToLeave -> TextButton(onClick = { wantsToLeave = true }) {
                Text("I still want to switch off", color = PresentColors.Muted, fontSize = 14.sp)
            }
            else -> LeaveSection(
                reason = reason,
                onReasonChange = { reason = it },
                onPauseInstead = onPauseInstead,
                onTurnOff = onTurnOff
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun QuestCard(quest: SocialQuest, onShuffle: () -> Unit, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("YOUR REAL-WORLD QUEST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PresentColors.Primary)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(quest.emoji, fontSize = 36.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(quest.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PresentColors.Ink)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(quest.detail, fontSize = 14.sp, color = PresentColors.Ink, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (quest.action != QuestAction.NONE) {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PresentColors.AllowedFg)
                    ) {
                        Text(if (quest.action == QuestAction.DIAL) "📞 Call now" else "💬 Message now")
                    }
                }
                OutlinedButton(onClick = onShuffle, shape = RoundedCornerShape(12.dp)) {
                    Text("🔀 Another quest", color = PresentColors.Primary)
                }
            }
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
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = PresentColors.Primary,
                unfocusedBorderColor = Color(0xFFDAD7F5)
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onPauseInstead,
            enabled = canLeave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4F00))
        ) {
            Text("Pause for 15 minutes instead", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onTurnOff,
            enabled = canLeave,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Turn off completely", color = PresentColors.BlockedFg, fontSize = 14.sp)
        }
    }
}
