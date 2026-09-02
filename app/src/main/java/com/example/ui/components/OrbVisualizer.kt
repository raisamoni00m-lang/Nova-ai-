package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassCyanLight
import com.example.ui.theme.GlassDarkSurfaceVariant
import com.example.ui.theme.GlassEmeraldGreen
import com.example.ui.theme.GlassIndigoDeep
import com.example.ui.theme.GlassIndigoLight
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassVioletAccent
import com.example.ui.theme.GlassVioletLight
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantVisualState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING
}

@Composable
fun OrbVisualizer(
    state: AssistantVisualState,
    audioAmplitude: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    size: Dp = 195.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbInfiniteTransition")

    // Continuous rotation for radiant gradient border
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantVisualState.THINKING) 2500 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    // Equalizer wave animation
    val eqWave by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbEqWave"
    )

    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(state, audioAmplitude) {
        val targetScale = when (state) {
            AssistantVisualState.LISTENING -> 1.0f + (audioAmplitude * 0.35f)
            AssistantVisualState.SPEAKING -> 1.0f + (audioAmplitude * 0.3f)
            AssistantVisualState.THINKING -> 1.08f
            AssistantVisualState.EXECUTING -> 1.1f
            AssistantVisualState.IDLE -> 1.0f
        }
        scaleAnim.animateTo(
            targetValue = targetScale,
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (this.size.minDimension / 2.6f) * breathingPulse * scaleAnim.value

            // 1. Ambient Frosted Glass Halo (Diffused Indigo/Violet/Cyan Blur)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassIndigoPrimary.copy(alpha = if (state == AssistantVisualState.LISTENING) 0.5f else 0.25f),
                        GlassVioletAccent.copy(alpha = 0.15f),
                        GlassCyanAccent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.9f
                ),
                radius = baseRadius * 1.9f,
                center = center
            )

            // 2. Multi-color Gradient Border Ring (Indigo -> Violet -> Cyan / Emerald when executing)
            val gradientColors = if (state == AssistantVisualState.EXECUTING) {
                listOf(
                    GlassEmeraldGreen,
                    GlassCyanLight,
                    GlassCyanAccent,
                    GlassIndigoLight,
                    GlassEmeraldGreen
                )
            } else {
                listOf(
                    GlassIndigoDeep,
                    GlassIndigoPrimary,
                    GlassVioletAccent,
                    GlassCyanAccent,
                    GlassCyanLight,
                    GlassIndigoDeep
                )
            }
            val sweepBrush = Brush.sweepGradient(
                colors = gradientColors,
                center = center
            )

            drawCircle(
                brush = sweepBrush,
                radius = baseRadius,
                center = center,
                style = Stroke(width = if (state == AssistantVisualState.EXECUTING) 5.dp.toPx() else 4.dp.toPx())
            )

            // Dynamic Orbital Energy Sparks during EXECUTING & THINKING
            if (state == AssistantVisualState.EXECUTING || state == AssistantVisualState.THINKING) {
                val sparkCount = 4
                for (s in 0 until sparkCount) {
                    val angleRad = Math.toRadians((rotation + (s * (360.0 / sparkCount))).toDouble())
                    val sparkRadius = baseRadius + (8.dp.toPx() * sin(eqWave * 3.14 + s).toFloat())
                    val sparkX = center.x + (sparkRadius * cos(angleRad)).toFloat()
                    val sparkY = center.y + (sparkRadius * sin(angleRad)).toFloat()
                    drawCircle(
                        color = if (state == AssistantVisualState.EXECUTING) GlassEmeraldGreen else GlassCyanLight,
                        radius = 3.5.dp.toPx(),
                        center = Offset(sparkX, sparkY)
                    )
                }
            }

            // 3. Frosted Obsidian Dark Core with Soft Inner Shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassDarkSurfaceVariant.copy(alpha = 0.9f),
                        GlassObsidianBackground.copy(alpha = 0.98f)
                    ),
                    center = center,
                    radius = baseRadius - 2.dp.toPx()
                ),
                radius = baseRadius - 2.dp.toPx(),
                center = center
            )

            // 4. Subtle Frosted Glass Rim Reflection
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = baseRadius - 3.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 5. Dynamic Audio Frequency Bars / Equalizer in Center
            val barCount = 5
            val barWidth = 4.5.dp.toPx()
            val spacing = 6.dp.toPx()
            val totalWidth = (barCount * barWidth) + ((barCount - 1) * spacing)
            val startX = center.x - (totalWidth / 2)

            val baseHeights = listOf(18.dp.toPx(), 34.dp.toPx(), 24.dp.toPx(), 30.dp.toPx(), 16.dp.toPx())
            val barColors = listOf(
                GlassIndigoLight,
                GlassVioletLight,
                GlassCyanLight,
                GlassIndigoPrimary,
                GlassVioletAccent
            )

            for (i in 0 until barCount) {
                val ampFactor = if (state == AssistantVisualState.LISTENING || state == AssistantVisualState.SPEAKING) {
                    0.4f + (audioAmplitude * 1.2f) + (0.25f * sin(eqWave * 3.14 + i).toFloat())
                } else if (state == AssistantVisualState.THINKING) {
                    0.5f + (0.5f * sin(eqWave * 6.28 + i).toFloat())
                } else {
                    0.35f
                }

                val barHeight = (baseHeights[i] * ampFactor).coerceIn(8.dp.toPx(), baseRadius * 0.9f)
                val barTop = center.y - (barHeight / 2)
                val barLeft = startX + (i * (barWidth + spacing))

                drawRoundRect(
                    color = barColors[i],
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}

