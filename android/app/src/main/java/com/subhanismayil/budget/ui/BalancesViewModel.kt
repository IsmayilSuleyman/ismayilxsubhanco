package com.subhanismayil.budget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subhanismayil.budget.BuildConfig
import com.subhanismayil.budget.data.BudgetApi
import com.subhanismayil.budget.data.Sheet2Parser
import com.subhanismayil.budget.data.Stats
import com.subhanismayil.budget.data.StatsComputer
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BalancesUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val stats: Stats? = null
)

class BalancesViewModel : ViewModel() {

    private val _state = MutableStateFlow(BalancesUiState())
    val state: StateFlow<BalancesUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = runCatching {
                // Fetch transactions (Sheet1) and summary (Sheet2) in parallel.
                val (rows, summaryValues) = kotlinx.coroutines.coroutineScope {
                    val txDeferred  = async { BudgetApi.fetchTransactions(BuildConfig.WEB_APP_URL) }
                    val sumDeferred = async { BudgetApi.fetchSummary(BuildConfig.WEB_APP_URL) }
                    Pair(txDeferred.await(), sumDeferred.await())
                }
                val stats = StatsComputer.compute(rows)
                val sheet2 = Sheet2Parser.parse(summaryValues)
                stats.copy(sheet2 = sheet2)
            }
            _state.value = result.fold(
                onSuccess = { BalancesUiState(loading = false, stats = it) },
                onFailure = { BalancesUiState(loading = false, error = it.message ?: "load failed") }
            )
        }
    }
}
