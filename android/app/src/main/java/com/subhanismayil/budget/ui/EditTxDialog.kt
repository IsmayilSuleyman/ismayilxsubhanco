package com.subhanismayil.budget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subhanismayil.budget.BuildConfig
import com.subhanismayil.budget.data.BudgetApi
import com.subhanismayil.budget.data.Categories
import com.subhanismayil.budget.data.People
import com.subhanismayil.budget.data.RecentTx
import com.subhanismayil.budget.data.TransactionRequest
import com.subhanismayil.budget.ui.theme.AccentPrimary
import com.subhanismayil.budget.ui.theme.BgLight
import com.subhanismayil.budget.ui.theme.Negative
import com.subhanismayil.budget.ui.theme.Positive
import com.subhanismayil.budget.ui.theme.SurfaceGlass
import com.subhanismayil.budget.ui.theme.TextPrimary
import com.subhanismayil.budget.ui.theme.TextSecondary
import com.subhanismayil.budget.ui.theme.colorForCategory
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditTxDialog(
    tx: RecentTx,
    onDismiss: () -> Unit,
    onSaved: (message: String) -> Unit,
    onDeleted: (message: String) -> Unit
) {
    var amount by remember(tx.id) { mutableStateOf("%.2f".format(abs(tx.amount))) }
    var isTopUp by remember(tx.id) { mutableStateOf(tx.isTopUp) }
    var who by remember(tx.id) { mutableStateOf(tx.who) }
    var categoryKey by remember(tx.id) {
        mutableStateOf(if (tx.isTopUp) null else tx.categoryKey)
    }
    var note by remember(tx.id) { mutableStateOf(tx.note) }
    var submitting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val canSubmit = remember(amount, who, categoryKey, isTopUp) {
        val n = amount.trim().replace(",", ".").toDoubleOrNull()
        n != null && n > 0 && who.isNotEmpty() &&
            (isTopUp && who != People.SHARED || (!isTopUp && categoryKey != null))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BgLight)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Edit transaction",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier
                        .background(Color(0x10000000), RoundedCornerShape(999.dp))
                        .padding(3.dp)
                ) {
                    EditToggleHalf("Expense", !isTopUp, Negative) { isTopUp = false }
                    EditToggleHalf("Top-up", isTopUp, Positive) {
                        isTopUp = true
                        if (who == People.SHARED) who = ""
                        categoryKey = null
                    }
                }

                SectionLabel("WHO")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val choices = if (isTopUp) People.INDIVIDUALS else People.ALL
                    choices.forEach { p ->
                        EditPill(p, who == p) { who = p }
                    }
                }

                if (!isTopUp) {
                    SectionLabel("Category")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Categories.EXPENSE.forEach { cat ->
                            EditChip(
                                label = "${cat.emoji} ${cat.key}",
                                selected = categoryKey == cat.key,
                                accent = colorForCategory(cat.key)
                            ) { categoryKey = cat.key }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Amount")
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = { Text("AZN", color = TextSecondary) },
                            colors = dlgFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Note")
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(200) },
                            singleLine = true,
                            placeholder = { Text("optional", color = TextSecondary) },
                            colors = dlgFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                error?.let {
                    Text(it, color = Negative, style = MaterialTheme.typography.bodyMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Negative),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Negative),
                        enabled = !submitting
                    ) { Text("Delete") }

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = onDismiss, enabled = !submitting) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            val parsed = amount.trim().replace(",", ".").toDoubleOrNull() ?: return@Button
                            val cat = if (isTopUp) Categories.TOP_UP_FULL
                            else Categories.EXPENSE.firstOrNull { it.key == categoryKey }?.full ?: return@Button
                            submitting = true
                            error = null
                            scope.launch {
                                val resp = runCatching {
                                    BudgetApi.submit(
                                        BuildConfig.WEB_APP_URL,
                                        TransactionRequest(
                                            amount = parsed,
                                            isTopUp = isTopUp,
                                            transactionBy = who,
                                            category = cat,
                                            note = note.trim(),
                                            action = "update",
                                            id = tx.id
                                        )
                                    )
                                }
                                submitting = false
                                val r = resp.getOrNull()
                                if (resp.isSuccess && r != null && r.ok) {
                                    onSaved("Updated")
                                } else {
                                    error = r?.error ?: resp.exceptionOrNull()?.message ?: "Failed"
                                }
                            }
                        },
                        enabled = canSubmit && !submitting,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete transaction?") },
            text = { Text("This permanently removes the row from the sheet.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        submitting = true
                        error = null
                        scope.launch {
                            val resp = runCatching {
                                BudgetApi.delete(BuildConfig.WEB_APP_URL, tx.id)
                            }
                            submitting = false
                            val r = resp.getOrNull()
                            if (resp.isSuccess && r != null && r.ok) {
                                onDeleted("Deleted")
                            } else {
                                error = r?.error ?: resp.exceptionOrNull()?.message ?: "Failed"
                            }
                        }
                    }
                ) { Text("Delete", color = Negative) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
            containerColor = SurfaceGlass
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary
    )
}

@Composable
private fun EditToggleHalf(
    label: String,
    selected: Boolean,
    activeFg: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) activeFg.copy(alpha = 0.18f) else Color.Transparent
    val fg = if (selected) activeFg else TextSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) { Text(label, color = fg, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun EditPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AccentPrimary else Color.Transparent
    val fg = if (selected) Color.White else TextPrimary
    val border = if (selected) AccentPrimary else Color(0x33000000)
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) { Text(label, color = fg) }
}

@Composable
private fun EditChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg = if (selected) accent.copy(alpha = 0.22f) else Color.Transparent
    val border = if (selected) accent else Color(0x33000000)
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = TextPrimary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(label, color = TextPrimary) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dlgFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentPrimary,
    unfocusedBorderColor = Color(0x33000000),
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentPrimary
)
