package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldXP
import com.example.ui.theme.PenaltyRed
import com.example.ui.theme.SuccessEmerald
import kotlin.math.roundToInt

/**
 * Animated Pulsing Flame with ember glow for active quiz streaks.
 */
@Composable
fun PulsingStreakFlame(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_flame_pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_glow"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFEA580C).copy(alpha = glowAlpha * 0.25f),
                        Color(0xFFEAB308).copy(alpha = glowAlpha * 0.35f)
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("pulsing_streak_flame")
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glowing aura backdrop
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(scale * 1.1f)
                    .clip(CircleShape)
                    .background(Color(0xFFF97316).copy(alpha = glowAlpha * 0.4f))
            )
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Streak Fire",
                tint = Color(0xFFF97316),
                modifier = Modifier
                    .size(18.dp)
                    .scale(scale)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${streak}x Streak (+5 Bonus)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFC2410C)
        )
    }
}

/**
 * Floating XP Badge with celebratory drift animation for quiz positive & negative rewards.
 */
@Composable
fun FloatingXpBadge(
    points: Int?,
    modifier: Modifier = Modifier
) {
    var previousPoints by remember { mutableStateOf<Int?>(null) }
    val offsetY = remember { Animatable(0f) }
    val opacity = remember { Animatable(1f) }

    LaunchedEffect(points) {
        if (points != null && points != 0) {
            previousPoints = points
            offsetY.snapTo(0f)
            opacity.snapTo(1f)
            // Animate drift upwards or downwards
            val targetOffset = if (points > 0) -40f else 30f
            offsetY.animateTo(targetOffset, animationSpec = tween(700, easing = FastOutSlowInEasing))
            opacity.animateTo(0f, animationSpec = tween(300))
        }
    }

    if (points != null && points != 0) {
        val isPositive = points > 0
        Box(
            modifier = modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .scale(opacity.value.coerceIn(0.5f, 1f))
                .clip(RoundedCornerShape(20.dp))
                .background(if (isPositive) SuccessEmerald else PenaltyRed)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("floating_xp_badge")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPositive) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (isPositive) "+$points XP" else "$points XP Penalty",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Mastery celebration effect with animated starbursts when mastering a card.
 */
@Composable
fun MasteryBurstEffect(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mastery_burst")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "burst_rot"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF34D399), Color(0xFF059669))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Mastered! +5 XP",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}
