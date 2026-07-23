package com.subhanismayil.budget.data

// Per-month aggregation of the transaction list, newest month first.
// Amount conventions follow StatsComputer: expense rows carry negative
// amounts (so "spent" is their negation), top-ups are positive
// (negative when the top-up is a withdrawal).
data class MonthStats(
    val year: Int,
    val month: Int, // 1..12
    val spent: Double,
    val topUp: Double,
    val categories: List<Pair<Category, Double>>, // > 0 only, sorted desc
    val ismayilSpent: Double,
    val subhanSpent: Double,
    val txCount: Int
) {
    val label: String get() = "${MonthlyStatsComputer.MONTH_FULL[month - 1]} $year"
    val shortLabel: String get() = MonthlyStatsComputer.MONTH_FULL[month - 1].take(3)
}

object MonthlyStatsComputer {

    val MONTH_FULL = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val MONTH_INDEX = mapOf(
        "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4,
        "May" to 5, "Jun" to 6, "Jul" to 7, "Aug" to 8,
        "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12
    )

    private val ISO_PREFIX = Regex("^(\\d{4})-(\\d{1,2})")

    // Handles both JS Date serialisations ("Wed May 13 2026 00:00:00 GMT+0400 (...)")
    // and plain ISO dates ("2026-05-13"). Returns (year, month) or null.
    fun yearMonthOf(date: String): Pair<Int, Int>? {
        val trimmed = date.trim()
        val parts = trimmed.split(Regex("\\s+"))
        if (parts.size >= 4) {
            val month = MONTH_INDEX[parts[1]]
            val year = parts[3].toIntOrNull()
            if (month != null && year != null) return year to month
        }
        ISO_PREFIX.find(trimmed)?.let { m ->
            val year = m.groupValues[1].toIntOrNull()
            val month = m.groupValues[2].toIntOrNull()
            if (year != null && month != null && month in 1..12) return year to month
        }
        return null
    }

    fun compute(transactions: List<RecentTx>): List<MonthStats> {
        data class Acc(
            var spent: Double = 0.0,
            var topUp: Double = 0.0,
            val catTotals: MutableMap<String, Double> = mutableMapOf(),
            var ismayil: Double = 0.0,
            var subhan: Double = 0.0,
            var txCount: Int = 0
        )

        val byMonth = linkedMapOf<Pair<Int, Int>, Acc>()
        for (tx in transactions) {
            val ym = yearMonthOf(tx.date) ?: continue
            val acc = byMonth.getOrPut(ym) { Acc() }
            acc.txCount++
            if (tx.isTopUp) {
                acc.topUp += tx.amount
                continue
            }
            val spent = -tx.amount
            acc.spent += spent
            if (tx.categoryKey.isNotEmpty()) {
                acc.catTotals[tx.categoryKey] = (acc.catTotals[tx.categoryKey] ?: 0.0) + spent
            }
            when (tx.who) {
                People.ISMAYIL -> acc.ismayil += spent
                People.SUBHAN -> acc.subhan += spent
                People.SHARED -> { acc.ismayil += spent / 2.0; acc.subhan += spent / 2.0 }
            }
        }

        return byMonth.entries
            .map { (ym, acc) ->
                MonthStats(
                    year = ym.first,
                    month = ym.second,
                    spent = acc.spent,
                    topUp = acc.topUp,
                    categories = acc.catTotals
                        .filter { it.value > 0.0049 }
                        .map { (key, amt) ->
                            val cat = Categories.EXPENSE.firstOrNull { it.key == key }
                                ?: Category(Categories.emojiFor(key), key)
                            cat to amt
                        }
                        .sortedByDescending { it.second },
                    ismayilSpent = acc.ismayil,
                    subhanSpent = acc.subhan,
                    txCount = acc.txCount
                )
            }
            .sortedWith(compareByDescending<MonthStats> { it.year }.thenByDescending { it.month })
    }
}
