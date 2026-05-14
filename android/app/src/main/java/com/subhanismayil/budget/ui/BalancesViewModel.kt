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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BalancesUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val stats: Stats? = null,
    val budgets: Map<String, Double> = emptyMap()
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
                val (rows, summaryValues, budgetMap) = kotlinx.coroutines.coroutineScope {
                    val txDeferred      = async { BudgetApi.fetchTransactions(BuildConfig.WEB_APP_URL) }
                    val sumDeferred     = async { BudgetApi.fetchSummary(BuildConfig.WEB_APP_URL) }
                    val budgetDeferred  = async {
                        runCatching { BudgetApi.fetchBudgets(BuildConfig.WEB_APP_URL) }.getOrDefault(emptyMap())
                    }
                    Triple(txDeferred.await(), sumDeferred.await(), budgetDeferred.await())
                }
                val stats = StatsComputer.compute(rows)
                val sheet2 = Sheet2Parser.parse(summaryValues)
                Pair(stats.copy(sheet2 = sheet2), budgetMap)
            }
            _state.value = result.fold(
                onSuccess = { (stats, budgets) -> BalancesUiState(loading = false, stats = stats, budgets = budgets) },
                onFailure = { BalancesUiState(loading = false, error = it.message ?: "load failed") }
            )
        }
    }

    fun saveBudgets(budgets: Map<String, Double>) {
        viewModelScope.launch {
            runCatching { BudgetApi.saveBudgets(BuildConfig.WEB_APP_URL, budgets) }
            _state.update { it.copy(budgets = budgets) }
        }
    }
}
