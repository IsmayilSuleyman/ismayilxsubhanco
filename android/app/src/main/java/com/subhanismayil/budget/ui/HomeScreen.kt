package com.subhanismayil.budget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subhanismayil.budget.data.Categories
import com.subhanismayil.budget.data.Category
import com.subhanismayil.budget.data.People
import com.subhanismayil.budget.data.Stats
import com.subhanismayil.budget.ui.theme.AccentPrimary
import com.subhanismayil.budget.ui.theme.AccentSecondary
import com.subhanismayil.budget.ui.theme.IsmayilColor
import com.subhanismayil.budget.ui.theme.Negative
import com.subhanismayil.budget.ui.theme.Positive
import com.subhanismayil.budget.ui.theme.SharedColor
import com.subhanismayil.budget.ui.theme.SubhanColor
import com.subhanismayil.budget.ui.theme.TextPrimary
import com.subhanismayil.budget.ui.theme.TextSecondary
import com.subhanismayil.budget.ui.theme.colorForCategory
import androidx.compose.ui.text.style.TextOverflow
import com.subhanismayil.budget.data.BudgetStatus
import com.subhanismayil.budget.data.budgetStatusFor
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onShowHistory: () -> Unit,
    onShowMonthly: () -> Unit = {},
    entryViewModel: EntryViewModel = viewModel(),
    balancesViewModel: BalancesViewModel = viewModel()
) {
    val entryState by entryViewModel.state.collectAsStateWithLifecycle()
    val balancesState by balancesViewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        entryViewModel.events.collect { ev ->
            val msg = when (ev) {
                is EntryEvent.Success -> {
                    balancesViewModel.refresh()
                    ev.message
                }
                is EntryEvent.Error -> ev.message
            }
            snackbarHost.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Content(
            padding = padding,
            entryState = entryState,
            balancesState = balancesState,
            onAmount = entryViewModel::setAmount,
            onTopUp = entryViewModel::setTopUp,
            onWithdrawal = entryViewModel::setWithdrawal,
            onWho = entryViewModel::setWho,
            onCategory = entryViewModel::setCategory,
            onNote = entryViewModel::setNote,
            onSubmit = entryViewModel::submit,
            onHistory = onShowHistory,
            onMonthly = onShowMonthly,
            onRefresh = balancesViewModel::refresh,
            budgets = balancesState.budgets,
            onSaveBudgets = balancesViewModel::saveBudgets
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    padding: PaddingValues,
    entryState: EntryUiState,
    balancesState: BalancesUiState,
    onAmount: (String) -> Unit,
    onTopUp: (Boolean) -> Unit,
    onWithdrawal: (Boolean) -> Unit,
    onWho: (String) -> Unit,
    onCategory: (String) -> Unit,
    onNote: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistory: () -> Unit,
    onMonthly: () -> Unit,
    onRefresh: () -> Unit,
    budgets: Map<String, Double>,
    onSaveBudgets: (Map<String, Double>) -> Unit
) {
    var showBudgets by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            title = "Shared Budget",
            subtitle = "${People.ISMAYIL} & ${People.SUBHAN}",
            trailing = { RefreshButton(loading = balancesState.loading, onClick = onRefresh) }
        )
        balancesState.error?.let { ErrorBanner(message = it, onRetry = onRefresh) }
        BalanceCard(stats = balancesState.stats, loading = balancesState.loading, onRefresh = onRefresh)
        ExpensesCard(
            stats = balancesState.stats,
            budgets = budgets,
            budgetsOpen = showBudgets,
            onBudgets = { showBudgets = !showBudgets },
            onMonthly = onMonthly
        )
        if (showBudgets) {
            BudgetsCard(
                stats = balancesState.stats,
                budgets = budgets,
                onSave = { onSaveBudgets(it); showBudgets = false },
                onDismiss = { showBudgets = false }
            )
        }
        AddTransactionCard(
            state = entryState,
            onAmount = onAmount,
            onTopUp = onTopUp,
            onWithdrawal = onWithdrawal,
            onWho = onWho,
            onCategory = onCategory,
            onNote = onNote,
            onSubmit = onSubmit,
            onHistory = onHistory
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Negative.copy(alpha = 0.10f))
            .border(1.dp, Negative.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = Negative,
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Couldn't refresh: $message",
            style = MaterialTheme.typography.bodySmall,
            color = Negative,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Retry",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Negative,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

// Balance-health gradient — full green→red spectrum, no purple.
// Priority (most urgent first):
//   one < 0          → red-orange  (danger)
//   both < 20        → amber       (warning)
//   one < 20         → olive-green (mild warning)
//   both > 50        → deep green  (excellent)
//   both 20–50       → mid green   (healthy)
private fun balanceGradient(stats: Stats?): Pair<Color, Color> {
    val a = stats?.perPerson?.get(People.ISMAYIL)?.balance ?: return Color(0xFF388E3C) to Color(0xFF81C784)
    val b = stats.perPerson[People.SUBHAN]?.balance ?: return Color(0xFF388E3C) to Color(0xFF81C784)
    return when {
        a < 0 || b < 0   -> Color(0xFFBF360C) to Color(0xFFE53935) // red-orange
        a < 20 && b < 20 -> Color(0xFFF57F17) to Color(0xFFFFD54F) // amber/yellow
        a < 20 || b < 20 -> Color(0xFF558B2F) to Color(0xFF8BC34A) // olive-green
        a > 50 && b > 50 -> Color(0xFF1B5E20) to Color(0xFF43A047) // deep green
        else             -> Color(0xFF388E3C) to Color(0xFF81C784) // mid green (20–50)
    }
}

@Composable
private fun BalanceCard(stats: Stats?, loading: Boolean, onRefresh: () -> Unit) {
    val (gradStart, gradEnd) = balanceGradient(stats)
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = gradStart.copy(alpha = 0.45f),
                spotColor = gradStart.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(colors = listOf(gradStart, gradEnd))
            )
            .clickable(onClick = onRefresh)
    ) {
        // Decorative highlights, fintech-card style
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 54.dp, y = (-48).dp)
                .size(150.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = 40.dp)
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-34).dp, y = 42.dp)
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Column(Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Total Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (stats != null) formatAznLarge(stats.total) else if (loading) "…" else "—",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PersonBalanceItem(
                    name = People.ISMAYIL,
                    amount = stats?.perPerson?.get(People.ISMAYIL)?.balance,
                    modifier = Modifier.weight(1f)
                )
                PersonBalanceItem(
                    name = People.SUBHAN,
                    amount = stats?.perPerson?.get(People.SUBHAN)?.balance,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PersonBalanceItem(name: String, amount: Double?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Text(
            if (amount != null) formatAznCompact(amount) else "—",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HeaderPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) AccentPrimary.copy(alpha = 0.12f) else Color(0x0A000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) AccentPrimary else TextSecondary
        )
    }
}

@Composable
private fun ExpensesCard(
    stats: Stats?,
    budgets: Map<String, Double>,
    budgetsOpen: Boolean,
    onBudgets: () -> Unit,
    onMonthly: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    AppCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Spending",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        "by category",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                HeaderPill("Months", active = false, onClick = onMonthly)
                HeaderPill("Budgets", active = budgetsOpen, onClick = onBudgets)
                if (stats != null) {
                    IconButton(
                        onClick = { showDetails = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Details",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Lighthouse advisory banner
            val warnings = stats?.categories.orEmpty().mapNotNull { (cat, amt) ->
                budgetStatusFor(amt, budgets[cat.key])
                    ?.takeIf { it != BudgetStatus.OK }
                    ?.let { cat to it }
            }
            if (warnings.isNotEmpty()) {
                val hasOver = warnings.any { it.second == BudgetStatus.OVER }
                val chipColor = if (hasOver) Color(0xFFEF4444) else Color(0xFFF59E0B)
                val overNames = warnings.filter { it.second == BudgetStatus.OVER }.joinToString { it.first.key }
                val nearNames = warnings.filter { it.second == BudgetStatus.NEAR }.joinToString { it.first.key }
                val label = buildString {
                    if (overNames.isNotEmpty()) append("Over budget: $overNames")
                    if (overNames.isNotEmpty() && nearNames.isNotEmpty()) append(" · ")
                    if (nearNames.isNotEmpty()) append("Near limit: $nearNames")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipColor.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = chipColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = chipColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val cats = stats?.categories.orEmpty().filter { it.second > 0.0049 }
                val totalSpent = (stats?.totalSpent ?: 1.0).coerceAtLeast(0.0001)
                if (cats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .background(Color(0x08000000), RoundedCornerShape(76.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                } else {
                    DonutChart(
                        slices = cats.map { (c, amt) -> DonutSlice(colorForCategory(c.key), amt) },
                        centerLabel = formatAznCompact(stats!!.totalSpent),
                        centerSubLabel = "spent",
                        diameter = 152.dp,
                        modifier = Modifier
                    )
                }
                Spacer(Modifier.width(20.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cats.take(6).forEach { (cat, amt) ->
                        val pct = (amt / totalSpent * 100).roundToInt().coerceAtLeast(1)
                        val limit = budgets[cat.key]
                        val budgetStatus = budgetStatusFor(amt, limit)
                        val fraction = limit?.let { (amt / it).toFloat().coerceIn(0f, 1f) }
                        val barColor = when (budgetStatus) {
                            BudgetStatus.OK   -> Color(0xFF10B981)
                            BudgetStatus.NEAR -> Color(0xFFF59E0B)
                            BudgetStatus.OVER -> Color(0xFFEF4444)
                            null              -> null
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(colorForCategory(cat.key), RoundedCornerShape(50))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cat.key,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "$pct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    formatAznCompact(amt),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                            }
                            if (fraction != null && barColor != null) {
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = barColor,
                                    trackColor = barColor.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetails && stats != null) {
        CategoryBreakdownDialog(
            categories = stats.categories,
            breakdown = stats.categoryBreakdown,
            onDismiss = { showDetails = false }
        )
    }
}

@Composable
private fun BudgetsCard(
    stats: Stats?,
    budgets: Map<String, Double>,
    onSave: (Map<String, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val spentMap = stats?.categories.orEmpty().associate { (cat, amt) -> cat.key to amt }
    var drafts by remember(budgets) {
        mutableStateOf(
            Categories.EXPENSE.associate { cat ->
                cat.key to (budgets[cat.key]?.let { "%.2f".format(it) } ?: "")
            }
        )
    }

    AppCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Budgets",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            SectionLabel("MONTHLY LIMITS")

            Categories.EXPENSE.forEach { cat ->
                val spent = spentMap[cat.key]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        cat.full,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (spent != null) {
                        Text(
                            formatAznCompact(spent),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    OutlinedTextField(
                        value = drafts[cat.key] ?: "",
                        onValueChange = { drafts = drafts + (cat.key to it) },
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("₼", color = TextSecondary) },
                        placeholder = { Text("—", color = TextSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AccentPrimary, AccentSecondary))
                        )
                        .clickable {
                            val result = drafts.mapNotNull { (key, text) ->
                                text.trim().replace(",", ".").toDoubleOrNull()
                                    ?.takeIf { it > 0.0 }
                                    ?.let { key to it }
                            }.toMap()
                            onSave(result)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save Budgets", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x1A000000))
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownDialog(
    categories: List<Pair<Category, Double>>,
    breakdown: Map<String, Pair<Double, Double>>,
    onDismiss: () -> Unit
) {
    val rows = categories.mapNotNull { (cat, _) ->
        val (i, s) = breakdown[cat.key] ?: return@mapNotNull null
        if (i < 0.005 && s < 0.005) return@mapNotNull null
        Triple(cat, i, s)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Spending by person", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // header
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        People.ISMAYIL,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(60.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        People.SUBHAN,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(60.dp)
                    )
                }
                rows.forEach { (cat, ismayil, subhan) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${cat.emoji} ${cat.key}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (ismayil > 0.005) formatAznCompact(ismayil) else "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ismayil > 0.005) TextPrimary else TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(60.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (subhan > 0.005) formatAznCompact(subhan) else "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (subhan > 0.005) TextPrimary else TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AccentPrimary) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionCard(
    state: EntryUiState,
    onAmount: (String) -> Unit,
    onTopUp: (Boolean) -> Unit,
    onWithdrawal: (Boolean) -> Unit,
    onWho: (String) -> Unit,
    onCategory: (String) -> Unit,
    onNote: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistory: () -> Unit
) {
    AppCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add Transaction",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TypeToggle(isTopUp = state.isTopUp, onChange = onTopUp)
            }

            if (state.isTopUp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("TYPE")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            label = "+ Deposit",
                            selected = !state.isWithdrawal,
                            onClick = { onWithdrawal(false) }
                        )
                        PillButton(
                            label = "− Withdrawal",
                            selected = state.isWithdrawal,
                            onClick = { onWithdrawal(true) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("BY WHO")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val choices = if (state.isTopUp) People.INDIVIDUALS else People.ALL
                    choices.forEach { who ->
                        PillButton(
                            label = who,
                            selected = state.who == who,
                            onClick = { onWho(who) },
                            accentColor = when (who) {
                                People.ISMAYIL -> IsmayilColor
                                People.SUBHAN  -> SubhanColor
                                else           -> SharedColor
                            }
                        )
                    }
                }
            }

            if (!state.isTopUp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("CATEGORY")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Categories.EXPENSE.forEach { cat ->
                            CategoryChip(
                                cat = cat,
                                selected = state.categoryKey == cat.key,
                                onClick = { onCategory(cat.key) }
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("AMOUNT")
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = onAmount,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = if (state.isTopUp) {
                            { Text(if (state.isWithdrawal) "−" else "+", color = TextSecondary) }
                        } else null,
                        suffix = { Text("₼", color = TextSecondary) },
                        placeholder = { Text("0.00", color = TextSecondary) },
                        textStyle = MaterialTheme.typography.titleLarge,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1.2f)) {
                    SectionLabel("NOTE")
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = onNote,
                        singleLine = true,
                        placeholder = { Text("Optional note", color = TextSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val canSubmit = state.canSubmit && !state.submitting
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AccentPrimary.copy(alpha = if (canSubmit) 1f else 0.45f),
                                    AccentSecondary.copy(alpha = if (canSubmit) 1f else 0.45f)
                                )
                            )
                        )
                        .clickable(enabled = canSubmit, onClick = onSubmit),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Submit Transaction", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x1A000000)),
                ) {
                    Text("History", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .width(3.dp)
                .height(10.dp)
                .background(AccentPrimary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
        )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun TypeToggle(isTopUp: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0x0A000000), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x10000000), RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TogglePart(label = "Expense", selected = !isTopUp, onClick = { onChange(false) })
        TogglePart(label = "Top-up", selected = isTopUp, onClick = { onChange(true) })
    }
}

@Composable
private fun TogglePart(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected)
                    Brush.horizontalGradient(listOf(AccentPrimary, AccentSecondary))
                else
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = AccentPrimary
) {
    val bg = if (selected) accentColor else Color(0x08000000)
    val fg = if (selected) Color.White else TextPrimary
    val border = if (selected) Color.Transparent else Color(0x18000000)
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CategoryChip(cat: Category, selected: Boolean, onClick: () -> Unit) {
    val accent = colorForCategory(cat.key)
    val bg = if (selected) accent else Color(0x08000000)
    val border = if (selected) Color.Transparent else Color(0x18000000)
    val labelColor = if (selected) Color.White else TextPrimary
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(cat.emoji, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(4.dp))
        Text(
            cat.key,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentPrimary,
    unfocusedBorderColor = Color(0x18000000),
    focusedContainerColor = Color.White.copy(alpha = 0.7f),
    unfocusedContainerColor = Color(0x08000000),
    cursorColor = AccentPrimary
)

internal fun formatAznLarge(n: Double): String {
    val absVal = "%.2f".format(abs(n))
    return (if (n < 0) "-" else "") + "₼" + absVal
}

internal fun formatAznCompact(n: Double): String {
    val absVal = "%.2f".format(abs(n))
    return (if (n < 0) "-" else "") + "₼" + absVal
}

internal fun signColor(n: Double): Color = when {
    n > 0.005 -> Positive
    n < -0.005 -> Negative
    else -> TextPrimary
}
