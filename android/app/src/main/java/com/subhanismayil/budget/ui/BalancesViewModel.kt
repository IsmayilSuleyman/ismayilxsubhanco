package com.subhanismayil.budget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhanismayil.budget.BuildConfig
import com.subhanismayil.budget.data.BudgetApi
import com.subhanismayil.budget.data.Sheet2Parser
import com.subhanismayil.budget.data.Sheet2Summary
import com.subhanismayil.budget.data.Stats
import com.subhanismayil.budget.data.StatsComputer
import com.subhanismayil.budget.data.TransactionRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BalancesUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val stats: Stats? = null,
    val budgets: Map<String, Double> = emptyMap()
)

// A transaction shown locally before (and just after) the sheet has it.
// Dropped once a refresh returns a row with its real ID.
private class PendingTx(val batch: String, val row: Map<String, String>) {
    var realId: String? = null
    var refreshesSinceSaved = 0
}

class BalancesViewModel : ViewModel() {

    private val _state = MutableStateFlow(BalancesUiState())
    val state: StateFlow<BalancesUiState> = _state.asStateFlow()

    private var serverRows: List<Map<String, String>> = emptyList()
    private var sheet2: Sheet2Summary? = null
    private val pending = mutableListOf<PendingTx>()
    private var refreshAgain = false

    init { refresh() }

    // Recompute everything on-device from the last fetched rows plus any
    // not-yet-synced local ones; cheap enough to run on every change.
    private fun publish() {
        val rows = serverRows + pending.map { p -> p.row + ("ID" to (p.realId ?: "")) }
        _state.update { it.copy(stats = StatsComputer.compute(rows).copy(sheet2 = sheet2)) }
    }

    fun refresh() {
        if (_state.value.loading) {
            refreshAgain = true
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                kotlinx.coroutines.coroutineScope {
                    val txDeferred      = async { BudgetApi.fetchTransactions(BuildConfig.WEB_APP_URL) }
                    val sumDeferred     = async { BudgetApi.fetchSummary(BuildConfig.WEB_APP_URL) }
                    val budgetDeferred  = async {
                        runCatching { BudgetApi.fetchBudgets(BuildConfig.WEB_APP_URL) }.getOrDefault(emptyMap())
                    }
                    Triple(txDeferred.await(), sumDeferred.await(), budgetDeferred.await())
                }
            }
            result.fold(
                onSuccess = { (rows, summaryValues, budgets) ->
                    serverRows = rows
                    sheet2 = Sheet2Parser.parse(summaryValues)
                    val ids = rows.mapTo(HashSet()) { it["ID"].orEmpty() }
                    // Saved rows leave once the sheet returns them (or after a
                    // few tries, so a missing ID can't keep us refreshing)
                    pending.removeAll { p ->
                        p.realId != null && (p.realId in ids || ++p.refreshesSinceSaved >= 3)
                    }
                    _state.update { it.copy(loading = false, budgets = budgets) }
                    publish()
                    // A synced transaction may have landed after this fetch started
                    if (pending.any { it.realId != null }) refreshAgain = true
                },
                // Keep the last good data visible when a refresh fails.
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "load failed") } }
            )
            if (refreshAgain && result.isSuccess) {
                refreshAgain = false
                refresh()
            }
            refreshAgain = false
        }
    }

    // Shows [requests] instantly, then saves them to the sheet in order. If
    // any fails, the ones already saved are deleted again, the local rows are
    // removed and [onFailure] gets the reason.
    fun submit(requests: List<TransactionRequest>, onFailure: (String) -> Unit) {
        val batch = UUID.randomUUID().toString()
        val now = Date()
        val date = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US).format(now)
        val time = SimpleDateFormat("'Sat Dec 30 1899' HH:mm:ss 'GMT'Z", Locale.US).format(now)
        val local = requests.map { req ->
            val signed = if (!req.isTopUp || req.isWithdrawal) -req.amount else req.amount
            PendingTx(batch, mapOf(
                "Date" to date,
                "Time" to time,
                "Transaction by" to req.transactionBy,
                "Transaction category" to req.category,
                "Amount (AZN)" to signed.toString(),
                "Note" to req.note
            ))
        }
        pending += local
        publish()

        viewModelScope.launch {
            val url = BuildConfig.WEB_APP_URL
            var failure: String? = null
            for ((req, p) in requests.zip(local)) {
                val result = runCatching { BudgetApi.submit(url, req) }
                val resp = result.getOrNull()
                if (resp != null && resp.ok) {
                    p.realId = resp.id ?: ""
                } else {
                    failure = result.exceptionOrNull()?.message ?: resp?.error ?: "unknown error"
                    break
                }
            }
            if (failure == null) {
                refresh()
                return@launch
            }
            // Undo whatever part of the batch reached the sheet
            val leftover = local.mapNotNull { it.realId?.takeIf(String::isNotEmpty) }
                .filterNot { id -> runCatching { BudgetApi.delete(url, id).ok }.getOrDefault(false) }
            pending.removeAll { it.batch == batch }
            publish()
            onFailure(
                if (leftover.isEmpty()) "Couldn't save — undone ($failure)"
                else "Part of it was saved but the rest failed — check History ($failure)"
            )
            if (leftover.isNotEmpty()) refresh()
        }
    }

    fun saveBudgets(budgets: Map<String, Double>) {
        viewModelScope.launch {
            runCatching { BudgetApi.saveBudgets(BuildConfig.WEB_APP_URL, budgets) }
            _state.update { it.copy(budgets = budgets) }
        }
    }
}
