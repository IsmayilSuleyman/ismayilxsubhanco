package com.subhanismayil.budget.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subhanismayil.budget.data.MonthStats
import com.subhanismayil.budget.data.People
import com.subhanismayil.budget.ui.theme.AccentPrimary
import com.subhanismayil.budget.ui.theme.AccentSecondary
import com.subhanismayil.budget.ui.theme.CardBorder
import com.subhanismayil.budget.ui.theme.IsmayilColor
import com.subhanismayil.budget.ui.theme.Negative
import com.subhanismayil.budget.ui.theme.Positive
import com.subhanismayil.budget.ui.theme.SubhanColor
import com.subhanismayil.budget.ui.theme.TextPrimary
import com.subhanismayil.budget.ui.theme.TextSecondary
import com.subhanismayil.budget.ui.theme.colorForCategory

private const val TREND_MONTHS = 6

@Composable
fun MonthlyScreen(viewModel: BalancesViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val months = state.stats?.monthly.orEmpty()
            when {
                state.loading && state.stats == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentPrimary)
                    }
                }
                state.error != null && state.stats == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Couldn't load: ${state.error}",
                            color = Negative,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> MonthsList(
                    months = months,
                    loading = state.loading,
                    onRefresh = viewModel::refresh
                )
            }
        }
    }
}

@Composable
private fun MonthsList(
    months: List<MonthStats>,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            ScreenHeader(
                title = "Months",
                subtitle = "Spending month by month",
                trailing = { RefreshButton(loading = loading, onClick = onRefresh) }
            )
        }
        if (months.isEmpty()) {
            item(key = "empty") {
                AppCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No monthly data yet", color = TextSecondary)
                    }
                }
            }
        } else {
            item(key = "trend") { TrendCard(months) }
            items(count = months.size, key = { "${months[it].year}-${months[it].month}" }) { idx ->
                MonthCard(m = months[idx], expandedInitially = idx == 0)
            }
        }
    }
}

// Bar chart of the most recent months, oldest on the left.
@Composable
private fun TrendCard(months: List<MonthStats>) {
    val shown = months.take(TREND_MONTHS).reversed()
    var selected by remember(shown.size, shown.lastOrNull()?.label) {
        mutableStateOf(shown.lastIndex)
    }
    val sel = shown.getOrNull(selected) ?: return
    val maxSpent = shown.maxOf { it.spent }.coerceAtLeast(0.01)

    AppCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Spending trend",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(sel.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    formatAznCompact(sel.spent),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                shown.forEachIndexed { i, m ->
                    TrendBar(
                        month = m,
                        index = i,
                        selected = i == selected,
                        fraction = (m.spent / maxSpent).toFloat().coerceIn(0f, 1f),
                        onClick = { selected = i },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (shown.size >= 2) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPrimary.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Average over ${shown.size} months",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatAznCompact(shown.sumOf { it.spent } / shown.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendBar(
    month: MonthStats,
    index: Int,
    selected: Boolean,
    fraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val animated by animateFloatAsState(
        targetValue = if (appeared) fraction.coerceAtLeast(0.03f) else 0.03f,
        animationSpec = tween(durationMillis = 650, delayMillis = index * 70, easing = FastOutSlowInEasing),
        label = "barHeight"
    )
    val barBrush = if (selected) {
        Brush.verticalGradient(listOf(AccentSecondary, AccentPrimary))
    } else {
        Brush.verticalGradient(
            listOf(AccentPrimary.copy(alpha = 0.22f), AccentPrimary.copy(alpha = 0.14f))
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .fillMaxHeight(animated)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(barBrush)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            month.shortLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AccentPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthCard(m: MonthStats, expandedInitially: Boolean) {
    var expanded by rememberSaveable(m.year, m.month) { mutableStateOf(expandedInitially) }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "chevron"
    )

    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(18.dp)
                .animateContentSize(tween(260, easing = FastOutSlowInEasing))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.label, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${m.txCount} transaction${if (m.txCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatAznCompact(m.spent),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        "spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevron)
                )
            }

            if (expanded) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder)
                )
                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (m.topUp > 0.005 || m.topUp < -0.005) {
                        StatChip(
                            label = "Top-ups",
                            value = (if (m.topUp > 0) "+" else "") + formatAznCompact(m.topUp),
                            color = if (m.topUp >= 0) Positive else Negative
                        )
                    }
                    StatChip(People.ISMAYIL, formatAznCompact(m.ismayilSpent), IsmayilColor)
                    StatChip(People.SUBHAN, formatAznCompact(m.subhanSpent), SubhanColor)
                }

                Spacer(Modifier.height(14.dp))

                if (m.categories.isEmpty()) {
                    Text(
                        "No categorised expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val monthTotal = m.spent.coerceAtLeast(0.01)
                        m.categories.forEach { (cat, amt) ->
                            CategoryShareRow(
                                emoji = cat.emoji,
                                name = cat.key,
                                amount = amt,
                                fraction = (amt / monthTotal).toFloat().coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Spacer(Modifier.width(5.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CategoryShareRow(emoji: String, name: String, amount: Double, fraction: Float) {
    val accent = colorForCategory(name)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(accent, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "$emoji $name",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatAznCompact(amount),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }
    }
}
