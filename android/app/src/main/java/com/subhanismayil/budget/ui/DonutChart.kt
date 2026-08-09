package com.subhanismayil.budget.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.subhanismayil.budget.ui.theme.TextPrimary
import com.subhanismayil.budget.ui.theme.TextSecondary

data class DonutSlice(val color: Color, val value: Double)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerLabel: String,
    centerSubLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp,
    strokeRatio: Float = 0.15f
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val total = slices.sumOf { it.value }.coerceAtLeast(0.0001)
            val stroke = size.minDimension * strokeRatio
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)

            // background track
            drawArc(
                color = Color(0x18000000),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )

            var start = -90f
            slices.forEach { slice ->
                val fullSweep = (slice.value / total * 360.0).toFloat()
                val animatedSweep = (fullSweep * progress.value - 1.5f).coerceAtLeast(0f)
                if (animatedSweep > 0f) {
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(slice.color.copy(alpha = 0.7f), slice.color),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        startAngle = start,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                }
                start += fullSweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerLabel,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
                color = TextPrimary
            )
            if (centerSubLabel.isNotEmpty()) {
                Text(
                    centerSubLabel,
                    style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.5.sp),
                    color = TextSecondary
                )
            }
        }
    }
}
