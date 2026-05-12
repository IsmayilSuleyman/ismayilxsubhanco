package com.subhanismayil.budget.ui.theme

import androidx.compose.ui.graphics.Color

// Stable color per category. Matches the legend swatches and the
// donut chart slices in HomeScreen.
val CategoryColors: Map<String, Color> = linkedMapOf(
    "Household"       to Color(0xFFF3E1B6), // beige
    "Groceries"       to Color(0xFF8FD66E), // green
    "Dining out"      to Color(0xFF8EC8E2), // sky blue
    "Utilities"       to Color(0xFFF4C84A), // amber
    "Subscriptions"   to Color(0xFFE85C5C), // red
    "Other"           to Color(0xFFB8855B), // brown
    "Entertainment"   to Color(0xFFF5B5C0), // pink
    "Transportation"  to Color(0xFF6E6BD3), // indigo
    "Healthcare"      to Color(0xFF06B6D4)  // cyan
)

fun colorForCategory(key: String): Color =
    CategoryColors[key] ?: Color(0xFFB0B6C4)
