package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ai.ToolCall
import com.example.ui.theme.GlassCyanAccent
import com.example.ui.theme.GlassCyanLight
import com.example.ui.theme.GlassDarkSurface
import com.example.ui.theme.GlassEmeraldGreen
import com.example.ui.theme.GlassIndigoLight
import com.example.ui.theme.GlassIndigoPrimary
import com.example.ui.theme.GlassObsidianBackground
import com.example.ui.theme.GlassTextSlate100
import com.example.ui.theme.GlassTextSlate300
import com.example.ui.theme.GlassTextSlate400
import com.example.ui.theme.GlassVioletAccent

@Composable
fun ExecutionOverlayHUD(
    activeTool: ToolCall?,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ExecutionOverlayTransition")

    // Rotation for orbiting halo
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HUDHaloRotation"
    )

    // Shimmer progress position
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HUDShimmer"
    )

    // Pulsing aura scale
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HUDAuraPulse"
    )

    AnimatedVisibility(
        visible = isExecuting || activeTool != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(16.dp, shape = RoundedCornerShape(20.dp), spotColor = GlassEmeraldGreen)
                .testTag("execution_overlay_hud"),
            shape = RoundedCornerShape(20.dp),
            color = GlassDarkSurface.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        GlassEmeraldGreen.copy(alpha = 0.8f),
                        GlassCyanLight.copy(alpha = 0.8f),
                        GlassIndigoLight.copy(alpha = 0.6f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GlassEmeraldGreen.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Animated Mini AI Core
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(36.dp)) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val radius = (size.minDimension / 2.2f) * auraScale

                                    // Pulsing Emerald Glow
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                GlassEmeraldGreen.copy(alpha = 0.6f),
                                                GlassCyanAccent.copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = radius * 1.4f,
                                        center = center
                                    )

                                    // Rotating Gradient Arc
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                GlassEmeraldGreen,
                                                GlassCyanLight,
                                                GlassIndigoLight,
                                                GlassEmeraldGreen
                                            )
                                        ),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = GlassEmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ASSISTIVE OVERLAY RUNNING",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp,
                                            color = GlassEmeraldGreen
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(GlassEmeraldGreen)
                                    )
                                }

                                val toolDisplay = activeTool?.let {
                                    "${it.toolName.replace('_', ' ').uppercase()} ${if (it.arguments.isNotEmpty()) it.arguments.values.firstOrNull() ?: "" else ""}"
                                } ?: "Performing automated system action..."

                                Text(
                                    text = toolDisplay,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = GlassTextSlate100
                                    )
                                )
                            }
                        }

                        // Right: Tribute badge for Owners Mizan & Ratul
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GlassIndigoPrimary.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassIndigoLight.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Mizan & Ratul",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = GlassIndigoLight
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shimmering Cyan-Emerald Animated Execution Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val barWidth = size.width
                            val progressWidth = barWidth * 0.4f
                            val startX = (barWidth + progressWidth) * shimmerOffset - progressWidth

                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        GlassCyanLight,
                                        GlassEmeraldGreen,
                                        GlassCyanLight,
                                        Color.Transparent
                                    ),
                                    startX = startX,
                                    endX = startX + progressWidth
                                ),
                                size = size
                            )
                        }
                    }
                }
            }
        }
    }
}
