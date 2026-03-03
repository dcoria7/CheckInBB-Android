package com.dc.checkinbb.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Animated baby bottle composable — mirrors iOS AnimatedBottleView.
 *
 * @param fillLevel  0.0 = just fed (bottle empty), 1.0 = overdue (bottle full)
 * @param primaryColor  theme primary color used for normal liquid
 * @param secondaryColor  theme secondary color used for normal liquid
 */
@Composable
fun AnimatedBottleView(
    fillLevel: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    // Continuous time animation for wave + bubbles
    val infiniteTransition = rememberInfiniteTransition(label = "bottle")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI * 100).toFloat(), // large enough to look continuous
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Liquid colors based on fill level (mirrors iOS logic)
    val liquidColors = when {
        fillLevel > 0.8f -> listOf(
            Color(0xFFF23333),
            Color(0xFFFF6650)
        )
        fillLevel > 0.5f -> listOf(
            Color(0xFFFFBF33),
            Color(0xFFFF9926)
        )
        else -> listOf(primaryColor, secondaryColor)
    }

    Canvas(modifier = modifier.size(width = 80.dp, height = 160.dp)) {
        val w = size.width
        val h = size.height

        // ── Metrics (proportional, mirrors iOS BottleMetrics) ──────────
        val nippleTopY    = h * 0.00f
        val nippleBottomY = h * 0.15f
        val neckTopY      = h * 0.15f
        val neckBottomY   = h * 0.22f
        val bodyTopY      = h * 0.22f
        val bodyBottomY   = h * 0.95f
        val baseY         = h * 0.97f

        val neckLeft  = w * 0.32f
        val neckRight = w * 0.68f
        val bodyLeft  = w * 0.18f
        val bodyRight = w * 0.82f

        // ── Body path ──────────────────────────────────────────────────
        val bodyPath = Path().apply {
            moveTo(neckLeft, neckTopY)
            lineTo(neckLeft, neckBottomY)
            quadraticBezierTo(bodyLeft, neckBottomY, bodyLeft, bodyTopY + (bodyBottomY - bodyTopY) * 0.08f)
            lineTo(bodyLeft, bodyBottomY)
            quadraticBezierTo(w * 0.5f, baseY, bodyRight, bodyBottomY)
            lineTo(bodyRight, bodyTopY + (bodyBottomY - bodyTopY) * 0.08f)
            quadraticBezierTo(bodyRight, neckBottomY, neckRight, neckBottomY)
            lineTo(neckRight, neckTopY)
            close()
        }

        // ── 1. Glass body background ───────────────────────────────────
        drawPath(
            path = bodyPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.10f)
                ),
                start = Offset(bodyLeft, bodyTopY),
                end   = Offset(bodyRight, bodyBottomY)
            )
        )

        // ── 2. Liquid with animated wave ───────────────────────────────
        val liquidLevel = (1.0f - fillLevel).coerceAtLeast(0.02f)
        val liquidAreaTop    = neckTopY
        val liquidAreaBottom = bodyBottomY
        val liquidAreaHeight = liquidAreaBottom - liquidAreaTop
        val surfaceY = liquidAreaBottom - liquidAreaHeight * liquidLevel

        if (liquidLevel > 0.01f) {
            val startX = bodyLeft - 2f
            val endX   = bodyRight + 2f
            val steps  = 40
            val waveHeight = 3.0f

            val liquidPath = Path().apply {
                moveTo(startX, surfaceY)
                for (i in 0..steps) {
                    val p = i.toFloat() / steps
                    val xPos = startX + (endX - startX) * p
                    val w1 = sin(p * Math.PI * 3 + time * 2.0).toFloat() * waveHeight
                    val w2 = sin(p * Math.PI * 2 + time * 1.5 + 1.0).toFloat() * waveHeight * 0.5f
                    lineTo(xPos, surfaceY + w1 + w2)
                }
                lineTo(bodyRight, bodyBottomY)
                quadraticBezierTo(w * 0.5f, baseY, bodyLeft, bodyBottomY)
                close()
            }

            clipPath(bodyPath) {
                drawPath(
                    path = liquidPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            liquidColors[0].copy(alpha = 0.85f),
                            liquidColors[1].copy(alpha = 0.95f)
                        ),
                        start = Offset(w * 0.5f, surfaceY),
                        end   = Offset(w * 0.5f, bodyBottomY)
                    )
                )
            }

            // ── 3. Bubbles ─────────────────────────────────────────────
            if (liquidLevel > 0.05f) {
                data class BubbleData(val xOff: Float, val sz: Float, val spd: Float, val ph: Float)
                val bubbles = listOf(
                    BubbleData(0.35f, 4f, 1.2f, 0.0f),
                    BubbleData(0.55f, 3f, 1.5f, 0.8f),
                    BubbleData(0.45f, 5f, 1.0f, 1.5f),
                    BubbleData(0.60f, 3f, 1.8f, 2.5f),
                    BubbleData(0.38f, 3.5f, 1.3f, 3.2f),
                )
                clipPath(bodyPath) {
                    for (b in bubbles) {
                        val cycleTime = 4.0f / b.spd
                        val tval = ((time + b.ph) % cycleTime) / cycleTime
                        val bx = w * b.xOff + sin((time * b.spd + b.ph).toDouble()).toFloat() * 6f
                        val by = liquidAreaBottom - (liquidAreaBottom - surfaceY) * tval
                        if (by > surfaceY && by < liquidAreaBottom) {
                            val opacity = ((1.0f - tval) * 2f).coerceIn(0f, 1f) * 0.6f
                            drawCircle(
                                color = Color.White.copy(alpha = opacity),
                                radius = b.sz / 2f,
                                center = Offset(bx, by)
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Measurement marks ───────────────────────────────────────
        val marks = listOf(0.3f, 0.5f, 0.7f, 0.85f)
        for (ratio in marks) {
            val y = bodyTopY + (bodyBottomY - bodyTopY) * (1f - ratio)
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(bodyRight - 10f, y),
                end   = Offset(bodyRight - 3f, y),
                strokeWidth = 0.8f
            )
        }

        // ── 5. Bottle outline ──────────────────────────────────────────
        drawPath(
            path = bodyPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.Gray.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.5f)
                ),
                start = Offset(bodyLeft, bodyTopY),
                end   = Offset(bodyRight, bodyBottomY)
            ),
            style = Stroke(width = 2f)
        )

        // ── 6. Neck ring ───────────────────────────────────────────────
        val ringLeft  = neckLeft - 4f
        val ringRight = neckRight + 4f
        val ringY     = neckTopY
        val ringH     = 6f
        val ringPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset(ringLeft, ringY - 1f), Size(ringRight - ringLeft, ringH)),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            )
        }
        drawPath(
            path = ringPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    Color.Gray.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.4f)
                ),
                start = Offset(ringLeft, ringY),
                end   = Offset(ringRight, ringY + ringH)
            )
        )
        drawPath(
            path = ringPath,
            color = Color.White.copy(alpha = 0.4f),
            style = Stroke(width = 1f)
        )

        // ── 7. Nipple ──────────────────────────────────────────────────
        val tipX = w * 0.5f
        val nipplePath = Path().apply {
            moveTo(neckLeft + 2f, nippleBottomY)
            quadraticBezierTo(neckLeft + 6f, nippleBottomY * 0.35f, tipX, nippleTopY + 2f)
            quadraticBezierTo(neckRight - 6f, nippleBottomY * 0.35f, neckRight - 2f, nippleBottomY)
            close()
        }
        drawPath(
            path = nipplePath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(red = 0.95f, green = 0.85f, blue = 0.70f, alpha = 0.90f),
                    Color(red = 0.88f, green = 0.75f, blue = 0.58f, alpha = 0.85f),
                    Color(red = 0.82f, green = 0.68f, blue = 0.50f, alpha = 0.80f)
                ),
                start = Offset(tipX, nippleTopY),
                end   = Offset(tipX, nippleBottomY)
            )
        )
        drawPath(
            path = nipplePath,
            color = Color(red = 0.75f, green = 0.60f, blue = 0.45f, alpha = 0.60f),
            style = Stroke(width = 1.5f)
        )

        // ── 8. Glass highlight ─────────────────────────────────────────
        val hlLeft   = bodyLeft + (bodyRight - bodyLeft) * 0.12f
        val hlRight  = bodyLeft + (bodyRight - bodyLeft) * 0.30f
        val hlTop    = bodyTopY + (bodyBottomY - bodyTopY) * 0.05f
        val hlBottom = bodyTopY + (bodyBottomY - bodyTopY) * 0.85f
        val hlMidY   = (hlTop + hlBottom) / 2f

        val hlPath = Path().apply {
            moveTo(hlLeft + 2f, hlTop)
            quadraticBezierTo(hlLeft - 3f, hlMidY, hlLeft, hlBottom)
            quadraticBezierTo((hlLeft + hlRight) / 2f, hlBottom + 5f, hlRight, hlBottom - 10f)
            quadraticBezierTo(hlRight + 2f, hlMidY, hlRight - 2f, hlTop)
            close()
        }
        drawPath(
            path = hlPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.00f)
                ),
                start = Offset(hlLeft, hlTop),
                end   = Offset(hlRight, hlBottom)
            )
        )
    }
}
