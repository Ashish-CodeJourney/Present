package com.mindguard.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mindguard.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PresentPalette {
    val NightTop: Color = Color(0xFF0F0C29)
    val NightBottom: Color = Color(0xFF302B63)
    val NightCard: Color = Color(0x1FFFFFFF)
    val OnNight: Color = Color(0xFFF7F5FF)
    val OnNightMuted: Color = Color(0xB3F7F5FF)
    val Sunrise: Color = Color(0xFFFF8A3D)
    val SunriseSoft: Color = Color(0xFFFFE2CC)
    val Mint: Color = Color(0xFF3DDC97)
    val MintSoft: Color = Color(0xFFDDF8EC)
    val Coral: Color = Color(0xFFFF5C7A)
    val CoralSoft: Color = Color(0xFFFFE0E6)
    val Cream: Color = Color(0xFFFFF8F1)
    val Ink: Color = Color(0xFF1B1838)
    val InkMuted: Color = Color(0xFF6E6A86)
    val CardLight: Color = Color(0xFFFFFFFF)
    val nightGradient: Brush = Brush.verticalGradient(listOf(NightTop, NightBottom))
    val sunriseGradient: Brush = Brush.horizontalGradient(listOf(Sunrise, Color(0xFFFFB463)))
}

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight): Font = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val PresentDisplayFont: FontFamily = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold)
)

object PresentType {
    val Display: TextStyle = TextStyle(
        fontFamily = PresentDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 60.sp
    )
    val Headline: TextStyle = TextStyle(
        fontFamily = PresentDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    )
    val Title: TextStyle = TextStyle(
        fontFamily = PresentDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )
    val Body: TextStyle = TextStyle(
        fontFamily = PresentDisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
    val Label: TextStyle = TextStyle(
        fontFamily = PresentDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
}

object PresentShapes {
    val Card: Shape = RoundedCornerShape(24.dp)
    val Pill: Shape = RoundedCornerShape(percent = 50)
    val Chip: Shape = RoundedCornerShape(14.dp)
}

object PresentMotion {
    fun <T> gentleSpring(): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

    const val StaggerMs: Int = 60
    const val EnterMs: Int = 350
}
