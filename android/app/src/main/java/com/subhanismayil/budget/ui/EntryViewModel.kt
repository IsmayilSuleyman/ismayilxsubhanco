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
    val isWithdrawal: Boolean = false, // only meaningful when isTopUp = true
    val who: String? = null,
    val categoryKey: String? = null,
    val note: String = "",
    // Shared expense paid from this person's own account (null = mutual
    // card): their balance is topped up first, then it's added as Shared.
    val paidBy: String? = null,
    val submitting: Boolean = false
) {
    // Whose balance gets the matching top-up, or null for a mutual-card expense
    val payer: String?
        get() = if (!isTopUp && who == People.SHARED) paidBy else null

    // Amount as signed double: negative when withdrawal top-up
    val signedAmount: Double?
        get() {
            val n = parseAmountInput(amount) ?: return null
            return if (isTopUp && isWithdrawal) -n else n
        }

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
            isWithdrawal = false,
            who = newWho,
            paidBy = null,
            categoryKey = if (v) null else current.categoryKey
        )
    }

    fun setWithdrawal(v: Boolean) {
        _state.value = _state.value.copy(isWithdrawal = v)
    }

    fun setWho(w: String) {
        _state.value = _state.value.copy(who = w, paidBy = null)
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

    // Shared expense paid from [payer]'s own account
    fun setSharedPaidBy(payer: String) {
        setWho(People.SHARED)
        _state.value = _state.value.copy(paidBy = payer)
    }

    fun submit(balances: BalancesViewModel) {
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
            isWithdrawal = s.isWithdrawal,
            transactionBy = who,
            category = category,
            note = s.note.trim()
        )
        val payer = s.payer
        // Own-account shared expense: the matching top-up goes first, like doing it by hand
        val requests = listOfNotNull(
            payer?.let {
                TransactionRequest(
                    amount = amount,
                    isTopUp = true,
                    transactionBy = it,
                    category = Categories.TOP_UP_FULL,
                    note = listOf("Own account", category, s.note.trim())
                        .filter(String::isNotEmpty).joinToString(" · ")
                )
            },
            req
        )
        // Shown and cleared instantly; the sheet catches up in the background.
        // If that fails, the form is refilled so it can be retried.
        balances.submit(requests) { msg ->
            _state.value = s.copy(submitting = false)
            _events.trySend(EntryEvent.Error(msg))
        }
        _state.value = EntryUiState(
            who = s.who,
            isTopUp = s.isTopUp,
            isWithdrawal = s.isWithdrawal
        )
        _events.trySend(EntryEvent.Success(if (payer != null) "Added top-up + expense" else "Added"))
    }
}
