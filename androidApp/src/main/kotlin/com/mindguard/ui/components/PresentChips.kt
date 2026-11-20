package com.mindguard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mindguard.ui.theme.PresentMotion
import com.mindguard.ui.theme.PresentPalette
import com.mindguard.ui.theme.PresentShapes
import com.mindguard.ui.theme.PresentType
import kotlinx.coroutines.delay

enum class ChipTone(val background: Color, val foreground: Color) {
    Blocked(PresentPalette.CoralSoft, Color(0xFFB8243F)),
    Allowed(PresentPalette.MintSoft, Color(0xFF17784F)),
    Neutral(Color(0xFFECE9E4), PresentPalette.InkMuted),
    Sunrise(PresentPalette.SunriseSoft, Color(0xFFA64A0F)),
    Night(PresentPalette.NightCard, PresentPalette.OnNight)
}

@Composable
fun SurfaceChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
    appearDelayMs: Int = 0,
    tag: String? = null
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(appearDelayMs.toLong())
        appear.animateTo(1f, PresentMotion.gentleSpring())
    }
    Row(
        modifier = modifier
            .graphicsLayer {
                val v = appear.value
                alpha = v.coerceIn(0f, 1f)
                scaleX = 0.8f + 0.2f * v
                scaleY = 0.8f + 0.2f * v
            }
            .clip(PresentShapes.Chip)
            .background(tone.background)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = PresentType.Label, color = tone.foreground)
        if (tag != null) {
            Spacer(modifier = Modifier.width(6.dp))
            MiniTag(tag)
        }
    }
}

@Composable
fun MiniTag(text: String, tone: ChipTone = ChipTone.Neutral) {
    Text(
        text,
        style = PresentType.Label.copy(fontSize = PresentType.Label.fontSize * 0.8f),
        color = tone.foreground,
        modifier = Modifier
            .clip(PresentShapes.Pill)
            .background(tone.background)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
