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

// Pre-computed summary read directly from Sheet2.
data class Sheet2Summary(
    val ismayilBalance: Double,
    val subhanBalance: Double,
    val totalBalance: Double,
    // category key → (total, ismayil, subhan)
    val categoryTotals: List<Triple<String, Double, Double>> // (total, ismayil, subhan) per category
)

object Sheet2Parser {
    private fun num(s: String): Double =
        s.replace(Regex("[^\\d.\\-]"), "").toDoubleOrNull() ?: 0.0

    // values = Sheet2!A1:D15 as List<List<String>>
    fun parse(values: List<List<String>>): Sheet2Summary {
        fun cell(row: Int, col: Int) = values.getOrNull(row)?.getOrNull(col) ?: ""

        val ismayilBalance = num(cell(0, 1))
        val subhanBalance  = num(cell(1, 1))
        val totalBalance   = num(cell(2, 1))

        // Rows 6-14 (0-indexed) = Sheet2 rows 7-15 = categories
        val cats = (6..14).mapNotNull { i ->
            val rawName = cell(i, 0).trim()
            if (rawName.isEmpty()) return@mapNotNull null
            val total   = num(cell(i, 1))
            val ismayil = num(cell(i, 2))
            val subhan  = num(cell(i, 3))
            Triple(total, ismayil, subhan)
        }

        return Sheet2Summary(ismayilBalance, subhanBalance, totalBalance, cats)
    }
}

data class Stats(
    val total: Double,
    val totalSpent: Double,
    val perPerson: Map<String, PersonStats>,
    val categories: List<Pair<Category, Double>>,
    // categoryKey → (İsmayıl spent, Sübhan spent); shared txs split 50/50
    val categoryBreakdown: Map<String, Pair<Double, Double>> = emptyMap(),
    val recent: List<RecentTx>,
    val txCount: Int,
    val sheet2: Sheet2Summary? = null
)

object StatsComputer {
    fun compute(rows: List<Map<String, String>>): Stats {
        val filtered = rows.filter { CsvParser.isExpenseRow(it) }
        val perPerson = People.INDIVIDUALS.associateWith { PersonStats() }
        val catTotals = Categories.EXPENSE.associate { it.key to 0.0 }.toMutableMap()
        val catBreakdown = mutableMapOf<String, Pair<Double, Double>>() // key → (ismayil, subhan)
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
                val spent = -amt
                catTotals[catKey] = catTotals.getValue(catKey) + spent
                totalSpent += spent
                val (curI, curS) = catBreakdown.getOrDefault(catKey, 0.0 to 0.0)
                catBreakdown[catKey] = when (who) {
                    People.ISMAYIL -> (curI + spent) to curS
                    People.SUBHAN  -> curI to (curS + spent)
                    People.SHARED  -> (curI + spent / 2.0) to (curS + spent / 2.0)
                    else           -> curI to curS
                }
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
            categoryBreakdown = catBreakdown,
            recent = recent,
            txCount = filtered.size
        )
    }
}
