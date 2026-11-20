package com.mindguard.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindguard.ui.components.ChipTone
import com.mindguard.ui.components.EmojiBadge
import com.mindguard.ui.components.NightSurface
import com.mindguard.ui.components.PresentCard
import com.mindguard.ui.components.PresentScreen
import com.mindguard.ui.components.PulseDot
import com.mindguard.ui.components.ScreenTopBar
import com.mindguard.ui.components.ShimmerLine
import com.mindguard.ui.components.Staggered
import com.mindguard.ui.components.SunrisePillButton
import com.mindguard.ui.components.rememberCountUp
import com.mindguard.ui.present.InsightsUiState
import com.mindguard.ui.present.formatTimeReclaimed
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType

private val TrackColor = Color(0xFFF1E9E0)

@Composable
fun StatsScreen(
    blockCountToday: Long,
    attemptCountToday: Long,
    totalBlocks: Long,
    totalAttempts: Long,
    currentStreak: Long,
    bestStreak: Long,
    instagramBlocks: Long = 0L,
    youtubeBlocks: Long = 0L,
    tiktokBlocks: Long = 0L,
    snapchatBlocks: Long = 0L,
    insightsState: InsightsUiState = InsightsUiState.Idle,
    onRequestInsight: () -> Unit = {},
    onBack: () -> Unit
) {
    var showToday by remember { mutableStateOf(true) }
    val blocks   = if (showToday) blockCountToday   else totalBlocks
    val attempts = if (showToday) attemptCountToday else totalAttempts
    val slipped  = (attempts - blocks).coerceAtLeast(0)
    val deflPct  = if (attempts > 0) (blocks * 100 / attempts) else 0L
    val blockRate = if (attempts > 0) blocks.toFloat() / attempts else 0f

    val appData = listOf(
        AppStat("Instagram", "📸", instagramBlocks, Color(0xFFE1306C)),
        AppStat("YouTube",   "▶️", youtubeBlocks,   Color(0xFFFF0000)),
        AppStat("TikTok",    "🎵", tiktokBlocks,    Color(0xFF444444)),
        AppStat("Snapchat",  "👻", snapchatBlocks,  Color(0xFFFFCC00))
    )
    val totalAppBlocks = appData.sumOf { it.count }
    val shownBlocks = rememberCountUp(blocks)

    PresentScreen(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
        ScreenTopBar(title = "Stats", onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))

        RangeToggle(showToday = showToday, onChange = { showToday = it })

        Spacer(modifier = Modifier.height(20.dp))

        Staggered(index = 0) {
            NightSurface(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏱️  Time reclaimed (approx.)", style = PresentType.Label, color = PresentPalette.OnNightMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    formatTimeReclaimed(shownBlocks),
                    style = PresentType.Display,
                    color = PresentPalette.OnNight,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "$blocks intercepts × 4.2 min of scrolling each",
                    style = PresentType.Label,
                    color = PresentPalette.OnNightMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 1) {
            InsightCard(state = insightsState, onRequestInsight = onRequestInsight)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 2) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailCard("🚫", "Videos blocked", blocks.toString(), Modifier.weight(1f))
                    DetailCard("👀", "Attempts", attempts.toString(), Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailCard("🎯", "Deflected", "$deflPct%", Modifier.weight(1f))
                    DetailCard("😬", "Slipped through", slipped.toString(), Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 3) {
            PresentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Block rate", style = PresentType.Body, color = PresentPalette.InkMuted)
                    Text("$deflPct%", style = PresentType.Title, color = PresentPalette.Ink)
                }
                Spacer(modifier = Modifier.height(10.dp))
                GradientBar(fraction = blockRate.coerceIn(0f, 1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 4) {
            PresentCard {
                Text("App breakdown", style = PresentType.Title, color = PresentPalette.Ink)
                Text("All-time distribution", style = PresentType.Label, color = PresentPalette.InkMuted)
                Spacer(modifier = Modifier.height(16.dp))

                if (totalAppBlocks == 0L) {
                    Text(
                        "No blocks recorded yet — the chart will appear once you get started!",
                        style = PresentType.Body,
                        color = PresentPalette.InkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppDonutChart(
                            data = appData,
                            total = totalAppBlocks,
                            modifier = Modifier.size(130.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            appData.filter { it.count > 0 }.forEach { app ->
                                val pct = (app.count * 100 / totalAppBlocks).toInt()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(app.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(app.name, style = PresentType.Label, color = PresentPalette.Ink)
                                        Text(
                                            "${app.count} blocks · $pct%",
                                            style = PresentType.Label,
                                            color = PresentPalette.InkMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appData.forEach { app ->
                        AppChip(app = app, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Staggered(index = 5) {
            PresentCard(color = PresentPalette.SunriseSoft) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StreakStat("🔥", "$currentStreak", "Current streak")
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(56.dp)
                            .background(PresentPalette.Sunrise.copy(alpha = 0.25f))
                    )
                    StreakStat("🏆", "$bestStreak", "Best streak")
                }
            }
        }
    }
}

private data class AppStat(val name: String, val emoji: String, val count: Long, val color: Color)

@Composable
private fun RangeToggle(showToday: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PresentShapes.Pill)
            .background(PresentPalette.CardLight)
            .padding(4.dp)
    ) {
        listOf("Today" to true, "All time" to false).forEach { (label, isToday) ->
            val selected = showToday == isToday
            val background by animateColorAsState(
                targetValue = if (selected) PresentPalette.Ink else Color.Transparent,
                animationSpec = tween(durationMillis = 250),
                label = "rangeBg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PresentShapes.Pill)
                    .background(background)
                    .clickable { onChange(isToday) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = PresentType.Label,
                    color = if (selected) PresentPalette.OnNight else PresentPalette.InkMuted
                )
            }
        }
    }
}

@Composable
private fun GradientBar(fraction: Float) {
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "blockRateBar"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(PresentShapes.Pill)
            .background(TrackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(PresentShapes.Pill)
                .background(PresentPalette.sunriseGradient)
        )
    }
}

@Composable
private fun AppDonutChart(data: List<AppStat>, total: Long, modifier: Modifier = Modifier) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = 1_000, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.18f
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val gap = 6f
        val progress = reveal.value

        drawArc(
            color = TrackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        data.filter { it.count > 0 }.fold(-90f) { startAngle, app ->
            val sweep = (app.count.toFloat() / total.toFloat()) * 360f
            drawArc(
                color = app.color,
                startAngle = startAngle + gap / 2f,
                sweepAngle = ((sweep - gap) * progress).coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle + sweep
        }
    }
}

@Composable
private fun AppChip(app: AppStat, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(PresentShapes.Chip)
            .background(app.color.copy(alpha = 0.10f))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(app.emoji, style = PresentType.Body)
        Spacer(modifier = Modifier.height(2.dp))
        Text(app.count.toString(), style = PresentType.Title, color = PresentPalette.Ink)
        Text(
            app.name,
            style = PresentType.Label.copy(fontSize = PresentType.Label.fontSize * 0.8f),
            color = PresentPalette.InkMuted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun DetailCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    PresentCard(modifier = modifier, contentPadding = 16.dp) {
        EmojiBadge(emoji = emoji, size = 36.dp, background = PresentPalette.Cream, style = PresentType.Body)
        Spacer(modifier = Modifier.height(10.dp))
        Text(value, style = PresentType.Headline, color = PresentPalette.Ink)
        Text(label, style = PresentType.Label, color = PresentPalette.InkMuted)
    }
}

@Composable
private fun StreakStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = PresentType.Headline)
        Text(value, style = PresentType.Headline, color = ChipTone.Sunrise.foreground)
        Text(label, style = PresentType.Label, color = PresentPalette.InkMuted)
    }
}

@Composable
private fun InsightCard(state: InsightsUiState, onRequestInsight: () -> Unit) {
    PresentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(emoji = "🧠", size = 40.dp, background = PresentPalette.Cream)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Weekly insight", style = PresentType.Title, color = PresentPalette.Ink)
                Text(
                    "Gemini reads your intercept counts (never your content)",
                    style = PresentType.Label,
                    color = PresentPalette.InkMuted
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            InsightsUiState.Idle -> SunrisePillButton(
                text = "Get my weekly insight from Gemini ✨",
                onClick = onRequestInsight,
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp
            )
            InsightsUiState.Loading -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(color = PresentPalette.Sunrise)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini is reading your week…", style = PresentType.Label, color = PresentPalette.InkMuted)
                }
                Spacer(modifier = Modifier.height(12.dp))
                ShimmerLine(widthFraction = 0.7f, height = 18.dp)
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerLine(widthFraction = 0.95f)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerLine(widthFraction = 0.8f)
            }
            is InsightsUiState.Ready -> Column {
                Text(state.headline, style = PresentType.Title, color = PresentPalette.Ink)
                Spacer(modifier = Modifier.height(6.dp))
                Text(state.insight, style = PresentType.Body, color = PresentPalette.InkMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "💡 ${state.suggestion}",
                    style = PresentType.Body,
                    color = ChipTone.Allowed.foreground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PresentShapes.Chip)
                        .background(PresentPalette.MintSoft)
                        .padding(14.dp)
                )
            }
            is InsightsUiState.Error -> Column {
                Text(state.message, style = PresentType.Body, color = ChipTone.Blocked.foreground)
                Spacer(modifier = Modifier.height(12.dp))
                SunrisePillButton(
                    text = "Try again",
                    onClick = onRequestInsight,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp
                )
            }
        }
    }
}
