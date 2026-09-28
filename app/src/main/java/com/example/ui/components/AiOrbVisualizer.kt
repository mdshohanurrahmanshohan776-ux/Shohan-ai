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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AssistantStatus
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingPink
import com.example.ui.theme.NeonCyan

@Composable
fun AiOrbVisualizer(
    status: AssistantStatus,
    soundLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Rotation angle for thinking state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse for idle
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Animated soundwave scale based on mic level
    val animatedRms = remember { Animatable(0f) }
    LaunchedEffect(soundLevel) {
        animatedRms.animateTo(
            targetValue = soundLevel,
            animationSpec = tween(80)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(190.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("ai_orb_button")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 3.4f

            when (status) {
                AssistantStatus.LISTENING -> {
                    val dynamicRadius = baseRadius * (1f + animatedRms.value * 0.45f)

                    // Outer soundwave glow rings
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.35f * (0.5f + animatedRms.value)), Color.Transparent),
                            center = center,
                            radius = dynamicRadius * 1.5f
                        ),
                        radius = dynamicRadius * 1.5f,
                        center = center
                    )

                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.6f),
                        radius = dynamicRadius * 1.25f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    drawCircle(
                        color = ElectricBlue.copy(alpha = 0.8f),
                        radius = dynamicRadius * 1.1f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Core Orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan, ElectricBlue, CyberPurple),
                            center = center,
                            radius = dynamicRadius
                        ),
                        radius = dynamicRadius,
                        center = center
                    )
                }

                AssistantStatus.PROCESSING -> {
                    val radius = baseRadius * 1.05f

                    // Rotating energy swirl rings
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonCyan, CyberPurple, GlowingPink, NeonCyan),
                            center = center
                        ),
                        radius = radius * 1.3f,
                        center = center,
                        style = Stroke(width = 3.5.dp.toPx())
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberPurple, ElectricBlue, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                AssistantStatus.SPEAKING -> {
                    val radius = baseRadius * idlePulse

                    // Speaking harmonic rings
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GlowingPink.copy(alpha = 0.4f), Color.Transparent),
                            center = center,
                            radius = radius * 1.4f
                        ),
                        radius = radius * 1.4f,
                        center = center
                    )

                    drawCircle(
                        color = GlowingPink,
                        radius = radius * 1.15f,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GlowingPink, CyberPurple, ElectricBlue),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                else -> { // IDLE or EXECUTING
                    val radius = baseRadius * idlePulse

                    // Ambient outer aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent),
                            center = center,
                            radius = radius * 1.35f
                        ),
                        radius = radius * 1.35f,
                        center = center
                    )

                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.4f),
                        radius = radius * 1.1f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Core Glowing Gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.9f), ElectricBlue, CyberPurple.copy(alpha = 0.8f)),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
        }

        // Center Icon Indicator
        val iconSize = 34.dp
        when (status) {
            AssistantStatus.LISTENING -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Listening",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            AssistantStatus.PROCESSING -> {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Thinking",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            AssistantStatus.SPEAKING -> {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speaking",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Tap to talk",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
