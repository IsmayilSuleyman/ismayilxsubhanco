package com.subhanismayil.budget.data

// Mirrors app.js's parseCSV / parseAmount / categoryKey logic so the
// Android app's balances match what the website renders from the same
// sheet.csv.

object CsvParser {

    fun parse(text: String): List<Map<String, String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"'); i++
                    } else inQuotes = false
                } else cell.append(c)
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { row.add(cell.toString()); cell.clear() }
                    '\n' -> { row.add(cell.toString()); rows.add(row); row = mutableListOf(); cell.clear() }
                    '\r' -> { /* skip */ }
                    else -> cell.append(c)
                }
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        if (rows.isEmpty()) return emptyList()
        val headers = rows[0].map { it.trim() }
        return rows.drop(1).map { r ->
            buildMap {
                headers.forEachIndexed { idx, h -> put(h, (r.getOrNull(idx) ?: "").trim()) }
            }
        }
    }

    fun parseAmount(s: String?): Double {
        if (s.isNullOrEmpty()) return 0.0
        val cleaned = s.replace("\\s+".toRegex(), "")
            .replace("₼", "")
            .replace(",", "")
        if (cleaned.isEmpty() || cleaned == "-") return 0.0
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private val leadingNonAlnum = Regex("^[^\\p{L}\\p{N}]+")

    fun categoryKey(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        return leadingNonAlnum.replace(raw, "").trim()
    }

    fun isTopUp(row: Map<String, String>): Boolean =
        categoryKey(row["Transaction category"]).equals("Top up", ignoreCase = true)

    fun isExpenseRow(row: Map<String, String>): Boolean {
        val who = row["Transaction by"] ?: return false
        return who in People.ALL
    }
}

data class PersonStats(var balance: Double = 0.0, var topup: Double = 0.0, var spent: Double = 0.0)

data class RecentTx(
    val id: String,            // empty for old rows without IDs
    val date: String,
    val time: String,
    val who: String,
    val categoryKey: String,
    val rawCategory: String,   // full "🥕 Groceries" / "➕ Top up" — used when sending back to the script
    val amount: Double,
    val note: String,
    val isTopUp: Boolean
) {
    val editable: Boolean get() = id.isNotEmpty()
}

data class Stats(
    val total: Double,
    val totalSpent: Double,
    val perPerson: Map<String, PersonStats>,
    val categories: List<Pair<Category, Double>>,
    val recent: List<RecentTx>,
    val txCount: Int
)

object StatsComputer {
    fun compute(rows: List<Map<String, String>>): Stats {
        val filtered = rows.filter { CsvParser.isExpenseRow(it) }
        val perPerson = People.INDIVIDUALS.associateWith { PersonStats() }
        val catTotals = Categories.EXPENSE.associate { it.key to 0.0 }.toMutableMap()
        var totalSpent = 0.0

        for (r in filtered) {
            val amt = CsvParser.parseAmount(r["Amount (AZN)"])
            val who = r["Transaction by"].orEmpty()
            val catKey = CsvParser.categoryKey(r["Transaction category"])

            if (CsvParser.isTopUp(r) && who in People.INDIVIDUALS) {
                val p = perPerson.getValue(who)
                p.topup += amt
                p.balance += amt
                continue
            }

            if (who == People.SHARED) {
                val half = amt / 2.0
                perPerson.getValue(People.SUBHAN).balance += half
                perPerson.getValue(People.ISMAYIL).balance += half
                perPerson.getValue(People.SUBHAN).spent += -half
                perPerson.getValue(People.ISMAYIL).spent += -half
            } else if (who in People.INDIVIDUALS) {
                val p = perPerson.getValue(who)
                p.balance += amt
                p.spent += -amt
            }

            if (catKey in catTotals) {
                catTotals[catKey] = catTotals.getValue(catKey) + (-amt)
                totalSpent += -amt
            }
        }

        val total = perPerson.getValue(People.SUBHAN).balance + perPerson.getValue(People.ISMAYIL).balance
        val cats = Categories.EXPENSE
            .map { it to (catTotals[it.key] ?: 0.0) }
            .sortedByDescending { it.second }

        val recent = filtered
            .map {
                RecentTx(
                    id = it["ID"].orEmpty(),
                    date = it["Date"].orEmpty(),
                    time = it["Time"].orEmpty(),
                    who = it["Transaction by"].orEmpty(),
                    categoryKey = CsvParser.categoryKey(it["Transaction category"]),
                    rawCategory = it["Transaction category"].orEmpty(),
                    amount = CsvParser.parseAmount(it["Amount (AZN)"]),
                    note = it["Note"].orEmpty(),
                    isTopUp = CsvParser.isTopUp(it)
                )
            }
            .sortedByDescending { it.date + " " + it.time }

        return Stats(
            total = total,
            totalSpent = totalSpent,
            perPerson = perPerson,
            categories = cats,
            recent = recent,
            txCount = filtered.size
        )
    }
}
