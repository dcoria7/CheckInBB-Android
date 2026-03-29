package com.dc.checkinbb.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.sin

/**
 * Animated baby bottle composable — mirrors iOS AnimatedBottleView.
 *
 * @param fillLevel  0.0 = just fed (bottle empty), 1.0 = overdue (bottle full)
 * @param primaryColor  theme primary color used for normal liquid
 * @param secondaryColor  theme secondary color used for normal liquid
 * @param isOverdue  when true, subtle pulse scale (matches iOS AnimatedBottleView when overdue)
 */
@Composable
fun AnimatedBottleView(
    fillLevel: Float,
    primaryColor: Color,
    secondaryColor: Color,
    isOverdue: Boolean = false,
    modifier: Modifier = Modifier
) {
    val overduePulse = rememberInfiniteTransition(label = "overduePulse")
    val pulseScale by overduePulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val drawScale = if (isOverdue) pulseScale else 1f

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

    // Liquid: siempre derivado del tema (primary / secondary); más contraste cuando sube el nivel
    val liquidColors = when {
        fillLevel > 0.8f -> listOf(
            lerp(primaryColor, secondaryColor, 0.88f),
            lerp(secondaryColor, primaryColor, 0.82f)
        )
        fillLevel > 0.5f -> listOf(
            lerp(primaryColor, secondaryColor, 0.55f),
            lerp(secondaryColor, primaryColor, 0.48f)
        )
        else -> listOf(primaryColor, secondaryColor)
    }

    Box(
        modifier = modifier.scale(drawScale)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // ── Metrics: wide short neck ("mamila") like iOS, not a protruding nipple ──
        val rimTopY = h * 0.03f
        val rimH = h * 0.018f
        val rimBottomY = rimTopY + rimH
        val mamilaBandBottomY = h * 0.11f
        val neckLeft = w * 0.24f
        val neckRight = w * 0.76f
        val neckBottomY = h * 0.20f
        val bodyTopY = neckBottomY
        val bodyBottomY = h * 0.94f
        val baseY = h * 0.97f
        val bodyLeft = w * 0.18f
        val bodyRight = w * 0.82f

        // Mamila: tonos a partir del tema (sin hex de referencia)
        val rimMetalLight = lerp(Color.White, primaryColor, 0.14f)
        val rimMetalDark = lerp(primaryColor, secondaryColor, 0.42f)
        val mamilaBandColor = lerp(primaryColor, Color.Black, 0.48f)
        val mamilaBandDark = lerp(secondaryColor, Color.Black, 0.58f)
        val rimGlint = lerp(Color.White, primaryColor, 0.18f)

        // ── Body path (flat wide opening at top) ───────────────────────
        val bodyPath = Path().apply {
            moveTo(neckLeft, rimTopY)
            lineTo(neckLeft, neckBottomY)
            quadraticBezierTo(bodyLeft, neckBottomY, bodyLeft, bodyTopY + (bodyBottomY - bodyTopY) * 0.08f)
            lineTo(bodyLeft, bodyBottomY)
            quadraticBezierTo(w * 0.5f, baseY, bodyRight, bodyBottomY)
            lineTo(bodyRight, bodyTopY + (bodyBottomY - bodyTopY) * 0.08f)
            quadraticBezierTo(bodyRight, neckBottomY, neckRight, neckBottomY)
            lineTo(neckRight, rimTopY)
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
        val liquidAreaTop = rimTopY
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
                color = lerp(primaryColor, secondaryColor, 0.35f).copy(alpha = 0.32f),
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
                    primaryColor.copy(alpha = 0.48f),
                    secondaryColor.copy(alpha = 0.36f),
                    primaryColor.copy(alpha = 0.42f)
                ),
                start = Offset(bodyLeft, bodyTopY),
                end   = Offset(bodyRight, bodyBottomY)
            ),
            style = Stroke(width = 1.5f)
        )

        // ── 6. Mamila iOS: thin metallic rim + dark band (occludes liquid at top) ──
        val rimRound = CornerRadius(2.5f, 2.5f)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(rimMetalLight, rimMetalDark, mamilaBandColor.copy(alpha = 0.3f)),
                startY = rimTopY,
                endY = rimBottomY
            ),
            topLeft = Offset(neckLeft, rimTopY),
            size = Size(neckRight - neckLeft, rimH),
            cornerRadius = rimRound
        )
        drawLine(
            color = rimGlint.copy(alpha = 0.88f),
            start = Offset(neckLeft + 2f, rimTopY + 0.6f),
            end = Offset(neckRight - 2f, rimTopY + 0.6f),
            strokeWidth = 1.1f
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(mamilaBandColor, mamilaBandDark),
                startY = rimBottomY,
                endY = mamilaBandBottomY
            ),
            topLeft = Offset(neckLeft, rimBottomY),
            size = Size(neckRight - neckLeft, mamilaBandBottomY - rimBottomY),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = Offset(neckLeft + 1f, mamilaBandBottomY),
            end = Offset(neckRight - 1f, mamilaBandBottomY),
            strokeWidth = 1f
        )

        // ── 7. Glass highlight (brillo lateral desde el primary del tema) ─
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
                    primaryColor.copy(alpha = 0.34f),
                    primaryColor.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                start = Offset(hlLeft, hlTop),
                end   = Offset(hlRight, hlBottom)
            )
        )
    }
    }
}
