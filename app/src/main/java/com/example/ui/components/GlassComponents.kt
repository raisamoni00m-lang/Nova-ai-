package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassCardBackground
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardSubtle
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassIndigoDeep
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassInnerBox
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassVioletAccent

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassCardBackground,
    borderColor: Color = GlassCardBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        content()
    }
}

@Composable
fun FrostedGlassInnerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    backgroundColor: Color = GlassInnerBox,
    borderColor: Color = GlassCardSubtle,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

/**
 * Renders an atmospheric diffuse gradient halo behind the content,
 * creating the frosted ambient glass aesthetic from the design reference.
 */
fun Modifier.ambientGlowBackdrop(
    primaryGlow: Color = GlassIndigoPrimary.copy(alpha = 0.15f),
    secondaryGlow: Color = GlassCyanAccent.copy(alpha = 0.08f)
): Modifier = this.drawBehind {
    val centerOffset = Offset(size.width * 0.5f, size.height * 0.35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primaryGlow, secondaryGlow, Color.Transparent),
            center = centerOffset,
            radius = size.minDimension * 0.9f
        ),
        radius = size.minDimension * 0.9f,
        center = centerOffset
    )
}
