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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.subhanismayil.budget.ui.theme.BgLight
import com.subhanismayil.budget.ui.theme.Negative
import com.subhanismayil.budget.ui.theme.Positive
import com.subhanismayil.budget.ui.theme.SurfaceGlass
import com.subhanismayil.budget.ui.theme.SurfaceGlassStrong
import com.subhanismayil.budget.ui.theme.TextPrimary
import com.subhanismayil.budget.ui.theme.TextSecondary
import com.subhanismayil.budget.ui.theme.colorForCategory
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onShowHistory: () -> Unit,
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
        containerColor = BgLight,
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Content(
            padding = padding,
            entryState = entryState,
            balancesState = balancesState,
            onAmount = entryViewModel::setAmount,
            onTopUp = entryViewModel::setTopUp,
            onWho = entryViewModel::setWho,
            onCategory = entryViewModel::setCategory,
            onNote = entryViewModel::setNote,
            onSubmit = entryViewModel::submit,
            onHistory = onShowHistory,
            onRefresh = balancesViewModel::refresh
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
    onWho: (String) -> Unit,
    onCategory: (String) -> Unit,
    onNote: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistory: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TitlePill()
        BalanceCard(stats = balancesState.stats, loading = balancesState.loading, onRefresh = onRefresh)
        ExpensesCard(stats = balancesState.stats)
        AddTransactionCard(
            state = entryState,
            onAmount = onAmount,
            onTopUp = onTopUp,
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
private fun TitlePill() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x1A000000), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "SHARED BUDGET",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceGlass, RoundedCornerShape(22.dp))
            .border(1.dp, Color(0x14000000), RoundedCornerShape(22.dp))
    ) { content() }
}

@Composable
private fun BalanceCard(stats: Stats?, loading: Boolean, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AccentPrimary, AccentSecondary)
                )
            )
            .clickable(onClick = onRefresh)
            .padding(24.dp)
    ) {
        Column {
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
private fun ExpensesCard(stats: Stats?) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Expenses this month",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val cats = stats?.categories.orEmpty().filter { it.second > 0.0049 }
                if (cats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0x08000000), RoundedCornerShape(70.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                } else {
                    DonutChart(
                        slices = cats.map { (c, amt) -> DonutSlice(colorForCategory(c.key), amt) },
                        centerLabel = formatAznCompact(stats!!.totalSpent),
                        centerSubLabel = "spent",
                        diameter = 140.dp,
                        modifier = Modifier
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cats.take(6).forEach { (cat, amt) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colorForCategory(cat.key), RoundedCornerShape(3.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                cat.key,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatAznCompact(amt),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionCard(
    state: EntryUiState,
    onAmount: (String) -> Unit,
    onTopUp: (Boolean) -> Unit,
    onWho: (String) -> Unit,
    onCategory: (String) -> Unit,
    onNote: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistory: () -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add Transaction",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                ExpenseTopUpToggle(isTopUp = state.isTopUp, onChange = onTopUp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("BY WHO")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val choices = if (state.isTopUp) People.INDIVIDUALS else People.ALL
                    choices.forEach { who ->
                        PillButton(
                            label = who,
                            selected = state.who == who,
                            onClick = { onWho(who) }
                        )
                    }
                }
            }

            if (!state.isTopUp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("CATEGORY")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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

            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("AMOUNT")
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = onAmount,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("₼", color = TextSecondary) },
                        placeholder = { Text("0.00", color = TextSecondary) },
                        textStyle = MaterialTheme.typography.titleLarge,
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
                        colors = textFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit && !state.submitting,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = AccentPrimary.copy(alpha = 0.5f)
                    )
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Submit Transaction", style = MaterialTheme.typography.titleMedium)
                    }
                }
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
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
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun ExpenseTopUpToggle(isTopUp: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0x0A000000), RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TogglePart(label = "Expense", selected = !isTopUp, onClick = { onChange(false) })
        TogglePart(label = "Top-up", selected = isTopUp, onClick = { onChange(true) })
    }
}

@Composable
private fun TogglePart(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color.White else Color.Transparent
    val elevation = if (selected) 2.dp else 0.dp
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AccentPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PillButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AccentPrimary else Color.Transparent
    val fg = if (selected) Color.White else TextPrimary
    val border = if (selected) AccentPrimary else Color(0x1A000000)
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun CategoryChip(cat: Category, selected: Boolean, onClick: () -> Unit) {
    val accent = colorForCategory(cat.key)
    val bg = if (selected) accent.copy(alpha = 0.15f) else Color.Transparent
    val border = if (selected) accent else Color(0x1A000000)
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(cat.emoji, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(6.dp))
        Text(cat.key, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentPrimary,
    unfocusedBorderColor = Color(0x1A000000),
    focusedContainerColor = Color.White.copy(alpha = 0.5f),
    unfocusedContainerColor = Color.Transparent,
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
