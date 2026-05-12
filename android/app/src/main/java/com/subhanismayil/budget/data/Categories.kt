package com.subhanismayil.budget.data

data class Category(val emoji: String, val key: String) {
    val full: String get() = "$emoji $key"
}

object Categories {
    val EXPENSE = listOf(
        Category("🥕", "Groceries"),
        Category("🍝", "Dining out"),
        Category("⚡", "Utilities"),
        Category("🚖", "Transportation"),
        Category("🖼", "Household"),
        Category("🤳", "Subscriptions"),
        Category("🎉", "Entertainment"),
        Category("🏥", "Healthcare"),
        Category("😀", "Other")
    )
    const val TOP_UP_FULL = "➕ Top up"

    fun emojiFor(key: String): String =
        EXPENSE.firstOrNull { it.key.equals(key, ignoreCase = true) }?.emoji ?: "💼"
}

object People {
    const val SUBHAN = "Sübhan"
    const val ISMAYIL = "İsmayıl"
    const val SHARED = "Shared"
    val ALL = listOf(SUBHAN, ISMAYIL, SHARED)
    val INDIVIDUALS = listOf(SUBHAN, ISMAYIL)
}
