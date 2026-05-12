package com.subhanismayil.budget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.subhanismayil.budget.BuildConfig
import com.subhanismayil.budget.data.BudgetApi
import com.subhanismayil.budget.data.Categories
import com.subhanismayil.budget.data.People
import com.subhanismayil.budget.data.Prefs
import com.subhanismayil.budget.data.TransactionRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class EntryUiState(
    val amount: String = "",
    val isTopUp: Boolean = false,
    val who: String? = null,
    val categoryKey: String? = null,
    val note: String = "",
    val submitting: Boolean = false
) {
    val canSubmit: Boolean
        get() {
            val n = parseAmountInput(amount) ?: return false
            if (n <= 0.0) return false
            val w = who ?: return false
            if (isTopUp) {
                if (w == People.SHARED) return false
                return true
            }
            return categoryKey != null
        }

    companion object {
        fun parseAmountInput(s: String): Double? {
            val cleaned = s.trim().replace(",", ".")
            if (cleaned.isEmpty()) return null
            return cleaned.toDoubleOrNull()
        }
    }
}

sealed class EntryEvent {
    data class Success(val message: String) : EntryEvent()
    data class Error(val message: String) : EntryEvent()
}

class EntryViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(EntryUiState())
    val state: StateFlow<EntryUiState> = _state.asStateFlow()

    private val _events = Channel<EntryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val last = Prefs.lastWho(getApplication()).first()
            if (last != null) {
                _state.value = _state.value.copy(who = last)
            }
        }
    }

    fun setAmount(s: String) {
        val filtered = s.filter { it.isDigit() || it == '.' || it == ',' }
        _state.value = _state.value.copy(amount = filtered)
    }

    fun setTopUp(v: Boolean) {
        val current = _state.value
        val newWho = if (v && current.who == People.SHARED) null else current.who
        _state.value = current.copy(
            isTopUp = v,
            who = newWho,
            categoryKey = if (v) null else current.categoryKey
        )
    }

    fun setWho(w: String) {
        _state.value = _state.value.copy(who = w)
        viewModelScope.launch {
            Prefs.setLastWho(getApplication(), w)
        }
    }

    fun setCategory(key: String) {
        _state.value = _state.value.copy(categoryKey = key)
    }

    fun setNote(s: String) {
        _state.value = _state.value.copy(note = s.take(200))
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit || s.submitting) return
        val amount = EntryUiState.parseAmountInput(s.amount) ?: return
        val who = s.who ?: return
        val category = if (s.isTopUp) {
            Categories.TOP_UP_FULL
        } else {
            val cat = Categories.EXPENSE.firstOrNull { it.key == s.categoryKey } ?: return
            cat.full
        }
        val req = TransactionRequest(
            amount = amount,
            isTopUp = s.isTopUp,
            transactionBy = who,
            category = category,
            note = s.note.trim()
        )
        _state.value = s.copy(submitting = true)
        viewModelScope.launch {
            val result = runCatching { BudgetApi.submit(BuildConfig.WEB_APP_URL, req) }
            val resp = result.getOrNull()
            if (result.isSuccess && resp != null && resp.ok) {
                _state.value = EntryUiState(
                    who = s.who,
                    isTopUp = s.isTopUp
                )
                _events.send(EntryEvent.Success("Added"))
            } else {
                val msg = when {
                    result.isFailure -> result.exceptionOrNull()?.message ?: "network error"
                    resp == null -> "no response"
                    else -> resp.error ?: "unknown error"
                }
                _state.value = s.copy(submitting = false)
                _events.send(EntryEvent.Error("Failed: $msg"))
            }
        }
    }
}
