package com.subhanismayil.budget.data

enum class BudgetStatus { OK, NEAR, OVER }

/** Returns null when no budget is set for this category. */
fun budgetStatusFor(spent: Double, limit: Double?): BudgetStatus? {
    if (limit == null || limit <= 0.0) return null
    return when {
        spent / limit >= 1.0  -> BudgetStatus.OVER
        spent / limit >= 0.75 -> BudgetStatus.NEAR
        else                  -> BudgetStatus.OK
    }
}
