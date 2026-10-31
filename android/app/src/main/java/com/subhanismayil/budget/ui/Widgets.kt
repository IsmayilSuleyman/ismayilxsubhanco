package com.subhanismayil.budget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subhanismayil.budget.ui.theme.AccentPrimary
import com.subhanismayil.budget.ui.theme.GlassBottom
import com.subhanismayil.budget.ui.theme.GlassEdgeBright
import com.subhanismayil.budget.ui.theme.GlassEdgeDim
import com.subhanismayil.budget.ui.theme.GlassShadow
import com.subhanismayil.budget.ui.theme.GlassTop
import com.subhanismayil.budget.ui.theme.MeshBaseBottom
import com.subhanismayil.budget.ui.theme.MeshBaseTop
import com.subhanismayil.budget.ui.theme.MeshCyan
import com.subhanismayil.budget.ui.theme.MeshIndigo
import com.subhanismayil.budget.ui.theme.MeshPink
import com.subhanismayil.budget.ui.theme.MeshViolet
import com.subhanismayil.budget.ui.theme.TextPrimary
import com.subhanismayil.budget.ui.theme.TextSecondary

// App-wide backdrop: soft colour blobs on a pale base, so the translucent
// glass surfaces above pick up colour from behind.
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MeshBaseTop, MeshBaseBottom)))
            .drawBehind {
                fun blob(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) = drawCircle(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                        center = Offset(size.width * cx, size.height * cy),
                        radius = size.width * r
                    ),
                    radius = size.width * r,
                    center = Offset(size.width * cx, size.height * cy)
                )
                blob(MeshIndigo, 0.42f, 0.05f, 0.08f, 0.85f)
                blob(MeshPink, 0.30f, 1.00f, 0.30f, 0.75f)
                blob(MeshCyan, 0.30f, 0.00f, 0.62f, 0.80f)
                blob(MeshViolet, 0.36f, 0.95f, 0.92f, 0.85f)
            },
        content = content
    )
}

// Frosted glass: translucent white fill that's lighter at the top, a bright
// top-left edge fading out, and a soft tinted shadow.
fun Modifier.glassSurface(shape: Shape, elevation: Dp = 10.dp): Modifier = this
    .shadow(elevation, shape, ambientColor = GlassShadow, spotColor = GlassShadow)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))
    .border(
        1.dp,
        Brush.linearGradient(listOf(GlassEdgeBright, GlassEdgeDim), start = Offset.Zero, end = Offset.Infinite),
        shape
    )

// Glossy sheen for coloured fills: a white highlight across the top half.
fun Modifier.gloss(strength: Float = 0.28f): Modifier = drawWithContent {
    drawContent()
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = strength),
            0.48f to Color.White.copy(alpha = strength * 0.25f),
            0.5f to Color.Transparent
        )
    )
}

// Frosted glass card used across all screens.
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(corner))
    ) { content() }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        trailing()
    }
}

// Small circular refresh control that turns into a spinner while loading.
@Composable
fun RefreshButton(loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .glassSurface(CircleShape, elevation = 6.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AccentPrimary
            )
        } else {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
