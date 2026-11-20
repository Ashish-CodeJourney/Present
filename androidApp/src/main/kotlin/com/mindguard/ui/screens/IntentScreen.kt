package com.mindguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mindguard.ui.components.BackPill
import com.mindguard.ui.components.PresentCard
import com.mindguard.ui.components.PresentScreen
import com.mindguard.ui.components.PulseDot
import com.mindguard.ui.components.SectionLabel
import com.mindguard.ui.components.ShimmerLine
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.components.SunrisePillButton
import com.mindguard.ui.components.presentTextFieldColors
import com.mindguard.ui.present.CompileUiState
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType

private val EXAMPLE_INTENTS = listOf(
    "No Reels or Shorts after 9 PM, keep my DMs",
    "Block all Shorts, keep my YouTube subscriptions",
    "Study mode: kill every short-video feed from 9 to 6"
)

private val ExampleBorder = Color(0xFFF3E3D3)

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

    PresentScreen(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)) {
        BackPill(onClick = onBack, enabled = !isLoading)

        Spacer(modifier = Modifier.height(20.dp))

        Staggered(index = 0) {
            Column {
                Text(
                    if (hasActivePolicy) "Edit your rules" else "What do you want from your phone?",
                    style = PresentType.Headline,
                    color = PresentPalette.Ink
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (hasActivePolicy) {
                        "Describe what to change. Gemini will update your current policy."
                    } else {
                        "Say it in your own words. Gemini turns it into rules you can review before anything changes."
                    },
                    style = PresentType.Body,
                    color = PresentPalette.InkMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Staggered(index = 1) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                textStyle = PresentType.Title,
                placeholder = {
                    Text("e.g. Keep IG DMs, kill Reels and Shorts after 9 PM", style = PresentType.Title)
                },
                shape = PresentShapes.Card,
                colors = presentTextFieldColors()
            )
        }

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                ThinkingCard()
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Staggered(index = 2) {
            Column {
                SectionLabel("Try one of these")
                EXAMPLE_INTENTS.forEachIndexed { index, example ->
                    Staggered(index = index + 3) {
                        ExampleChip(
                            text = example,
                            selected = prompt == example,
                            enabled = !isLoading,
                            onClick = { onPromptChange(example) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        when (compileState) {
            is CompileUiState.Rejected -> {
                Spacer(modifier = Modifier.height(12.dp))
                MessageCard(
                    emoji = "🤔",
                    title = "Present can't do that one",
                    body = compileState.reason,
                    containerColor = PresentPalette.SunriseSoft,
                    titleColor = Color(0xFFA64A0F)
                )
            }
            is CompileUiState.Error -> {
                Spacer(modifier = Modifier.height(12.dp))
                MessageCard(
                    emoji = "📡",
                    title = "Something went wrong",
                    body = compileState.message,
                    containerColor = PresentPalette.CoralSoft,
                    titleColor = Color(0xFFB8243F)
                )
            }
            else -> Unit
        }

        Spacer(modifier = Modifier.height(20.dp))

        SunrisePillButton(
            text = "Compile with Gemini ✨",
            onClick = onCompile,
            enabled = prompt.isNotBlank() && !isLoading,
            loading = isLoading,
            loadingText = "Gemini is compiling your policy…",
            modifier = Modifier.fillMaxWidth(),
            height = 60.dp
        )
    }
}

@Composable
private fun ExampleChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PresentShapes.Chip)
            .background(if (selected) PresentPalette.SunriseSoft else PresentPalette.CardLight)
            .border(
                BorderStroke(1.dp, if (selected) PresentPalette.Sunrise else ExampleBorder),
                PresentShapes.Chip
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💬", style = PresentType.Body)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = PresentType.Body, color = PresentPalette.Ink)
    }
}

@Composable
private fun ThinkingCard() {
    PresentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseDot(color = PresentPalette.Sunrise)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gemini is reading your intent…", style = PresentType.Label, color = PresentPalette.InkMuted)
        }
        Spacer(modifier = Modifier.height(14.dp))
        ShimmerLine(widthFraction = 0.9f)
        Spacer(modifier = Modifier.height(10.dp))
        ShimmerLine(widthFraction = 0.65f)
        Spacer(modifier = Modifier.height(10.dp))
        ShimmerLine(widthFraction = 0.8f)
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
    PresentCard(color = containerColor, contentPadding = 18.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Text(emoji, style = PresentType.Headline)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = PresentType.Title.copy(fontSize = PresentType.Body.fontSize), color = titleColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(body, style = PresentType.Body, color = PresentPalette.Ink)
            }
        }
    }
}
