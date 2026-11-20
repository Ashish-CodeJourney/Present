package com.mindguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import com.mindguard.ui.present.CompileUiState

private val EXAMPLE_INTENTS = listOf(
    "No Reels or Shorts after 9 PM, keep my DMs",
    "Block all Shorts, keep my YouTube subscriptions",
    "Study mode: kill every short-video feed from 9 to 6"
)

@Composable
fun IntentScreen(
    prompt: String,
    compileState: CompileUiState,
    hasActivePolicy: Boolean,
    onPromptChange: (String) -> Unit,
    onCompile: () -> Unit,
    onBack: () -> Unit
) {
    val isLoading = compileState == CompileUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PresentColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack, enabled = !isLoading) {
            Text("← Back", color = PresentColors.Primary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            if (hasActivePolicy) "Edit your rules" else "What do you want from your phone?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = PresentColors.Ink,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (hasActivePolicy) {
                "Describe what to change. Gemini will update your current policy."
            } else {
                "Say it in your own words. Gemini turns it into rules you can review before anything changes."
            },
            fontSize = 14.sp,
            color = PresentColors.Muted
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            placeholder = { Text("e.g. Keep IG DMs, kill Reels and Shorts after 9 PM") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = PresentColors.Primary,
                unfocusedBorderColor = Color(0xFFDAD7F5)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Try one of these", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PresentColors.Muted)
        Spacer(modifier = Modifier.height(8.dp))

        EXAMPLE_INTENTS.forEach { example ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8E6FF),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading) { onPromptChange(example) }
            ) {
                Text(
                    "💬  $example",
                    fontSize = 13.sp,
                    color = PresentColors.Ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (compileState) {
            is CompileUiState.Rejected -> MessageCard(
                emoji = "🤔",
                title = "Present can't do that one",
                body = compileState.reason,
                containerColor = Color(0xFFFFF7ED),
                titleColor = Color(0xFFB45309)
            )
            is CompileUiState.Error -> MessageCard(
                emoji = "📡",
                title = "Something went wrong",
                body = compileState.message,
                containerColor = Color(0xFFFFEBEE),
                titleColor = Color(0xFFC62828)
            )
            else -> Unit
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCompile,
            enabled = prompt.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PresentColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Gemini is compiling your policy…", fontSize = 15.sp)
            } else {
                Text("Compile with Gemini ✨", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MessageCard(
    emoji: String,
    title: String,
    body: String,
    containerColor: Color,
    titleColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(body, fontSize = 14.sp, color = PresentColors.Ink)
            }
        }
    }
}
