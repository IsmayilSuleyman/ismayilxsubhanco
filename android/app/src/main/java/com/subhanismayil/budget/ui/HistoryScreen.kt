package com.subhanismayil.budget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subhanismayil.budget.data.Categories
import com.subhanismayil.budget.data.People
import com.subhanismayil.budget.data.RecentTx
import com.subhanismayil.budget.ui.theme.AccentPrimary
import com.subhanismayil.budget.ui.theme.BgLight
import com.subhanismayil.budget.ui.theme.Negative
import com.subhanismayil.budget.ui.theme.SurfaceGlass
import com.subhanismayil.budget.ui.theme.TextPrimary
import com.subhanismayil.budget.ui.theme.TextSecondary
import com.subhanismayil.budget.ui.theme.colorForCategory
import kotlinx.coroutines.launch

private const val EDITABLE_TOP_N = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: BalancesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<RecentTx?>(null) }

    Scaffold(
        containerColor = BgLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Body(padding, state, onEdit = { editing = it })
    }

    editing?.let { tx ->
        EditTxDialog(
            tx = tx,
            onDismiss = { editing = null },
            onSaved = { msg ->
                editing = null
                viewModel.refresh()
                scope.launch { snackbarHost.showSnackbar(msg) }
            },
            onDeleted = { msg ->
                editing = null
                viewModel.refresh()
                scope.launch { snackbarHost.showSnackbar(msg) }
            }
        )
    }
}

@Composable
private fun Body(
    padding: PaddingValues,
    state: BalancesUiState,
    onEdit: (RecentTx) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
    ) {
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
            else -> {
                val recent = state.stats?.recent.orEmpty()
                if (recent.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No transactions yet", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(recent.size) { idx ->
                            val tx = recent[idx]
                            val editable = idx < EDITABLE_TOP_N && tx.editable
                            TxRow(
                                tx = tx,
                                editable = editable,
                                onClick = { if (editable) onEdit(tx) }
                            )
                        }
                        if (recent.size > EDITABLE_TOP_N) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Older transactions are read-only",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TxRow(tx: RecentTx, editable: Boolean, onClick: () -> Unit) {
    val emoji = if (tx.isTopUp) "➕" else
        Categories.EXPENSE.firstOrNull { it.key == tx.categoryKey }?.emoji ?: "💼"
    val accent = if (tx.isTopUp) AccentPrimary else colorForCategory(tx.categoryKey)

    val rowModifier = Modifier
        .fillMaxWidth()
        .background(SurfaceGlass, RoundedCornerShape(16.dp))
        .border(1.dp, Color(0x14000000), RoundedCornerShape(16.dp))
        .then(if (editable) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 14.dp, vertical = 12.dp)

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (tx.isTopUp) "Top up" else tx.categoryKey.ifEmpty { "—" },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tx.date, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (tx.time.isNotEmpty()) {
                    Text("  ·  ${tx.time}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                WhoChip(tx.who)
                if (tx.note.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tx.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatAznCompact(tx.amount),
                style = MaterialTheme.typography.titleMedium,
                color = if (tx.amount < -0.005) Negative else TextPrimary
            )
            if (editable) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit",
                        tint = AccentPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Tap to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun WhoChip(who: String) {
    val color = when (who) {
        People.SUBHAN -> Color(0xFF8B5CF6)
        People.ISMAYIL -> Color(0xFF6366F1)
        else -> TextSecondary
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(who, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
