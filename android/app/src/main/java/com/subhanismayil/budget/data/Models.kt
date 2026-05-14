package com.subhanismayil.budget.data

import kotlinx.serialization.Serializable

@Serializable
data class TransactionRequest(
    val amount: Double,
    val isTopUp: Boolean,
    val transactionBy: String,
    val category: String,
    val note: String = "",
    val action: String = "create", // "create" | "update" | "delete"
    val id: String? = null         // required for update/delete
)

@Serializable
data class TransactionResponse(
    val ok: Boolean,
    val error: String? = null,
    val row: Int? = null,
    val id: String? = null,
    val deleted: String? = null
)

@Serializable
data class BudgetsRequest(
    val action: String = "saveBudgets",
    val budgets: Map<String, Double>
)
