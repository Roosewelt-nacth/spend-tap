package com.austin.spendtap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.austin.spendtap.model.MonthKey
import com.austin.spendtap.model.SavingsKind
import com.austin.spendtap.model.SavingsPlan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun kindIcon(k: SavingsKind): ImageVector = when (k) {
    SavingsKind.SIP -> Icons.Rounded.TrendingUp
    SavingsKind.RD -> Icons.Rounded.CalendarMonth
    SavingsKind.FD -> Icons.Rounded.Lock
    SavingsKind.PPF -> Icons.Rounded.AccountBalance
    SavingsKind.GOLD -> Icons.Rounded.Toll
    SavingsKind.OTHER -> Icons.Rounded.Paid
}

private fun ordinal(n: Int) = when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

@Composable
fun SavingsScreen(vm: MainViewModel, pad: PaddingValues) {
    val plans by vm.plans.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SavingsPlan?>(null) }
    var creating by remember { mutableStateOf(false) }

    val monthly = plans.filter { it.isActive }.sumOf { it.monthlyAmount }
    val income = settings?.monthlyIncome ?: 0.0

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, pad.calculateTopPadding() + 12.dp, 16.dp, pad.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassCard(corner = 32.dp) {
                    Eyebrow("SET ASIDE EVERY MONTH")
                    AnimatedMoney(monthly, MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 34.sp))
                    Text(
                        if (income > 0) "${(monthly / income * 100).toInt()}% of your income · ${rupees(monthly * 12)} a year"
                        else "${rupees(monthly * 12)} a year",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
            if (plans.isEmpty()) item {
                Text(
                    "Add your SIPs, RDs and other monthly savings.\nThey are set aside before spending, so \"left to spend\" stays honest.",
                    Modifier.fillMaxWidth().padding(top = 24.dp), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.6f),
                )
            }
            items(plans, key = { it.id }) { plan -> PlanCard(plan, Modifier.animateItem()) { editing = plan } }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = pad.calculateBottomPadding() + 20.dp),
        ) { Icon(Icons.Rounded.Add, "Add plan") }
    }

    if (creating) PlanEditor(null, { creating = false }, { vm.upsert(it); creating = false }, {})
    editing?.let { p ->
        PlanEditor(p, { editing = null }, { vm.upsert(it); editing = null }, { vm.delete(p); editing = null })
    }
}

@Composable
private fun PlanCard(plan: SavingsPlan, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(kindIcon(plan.kind), null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(plan.name, fontWeight = FontWeight.SemiBold)
                Text("${plan.kind.title} · on the ${plan.dayOfMonth}${ordinal(plan.dayOfMonth)}",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(rupees(plan.monthlyAmount), fontWeight = FontWeight.SemiBold)
                Text(if (plan.isActive) "per month" else "paused", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
        Text("Put in so far: ${rupees(plan.contributed())} over ${plan.monthsContributed()} months",
            Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
        plan.progress()?.let { p ->
            LinearProgressIndicator(progress = { p.toFloat() }, Modifier.fillMaxWidth().padding(top = 8.dp), color = Color(0xFF6EE7B7))
            Text("Goal ${rupees(plan.targetAmount ?: 0.0)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        }
        plan.projectedValue(5)?.let { v ->
            Text("≈ ${rupees(v, compact = true)} in 5 years at ${plan.expectedReturnPct}% a year (estimate)",
                Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanEditor(original: SavingsPlan?, onDismiss: () -> Unit, onSave: (SavingsPlan) -> Unit, onDelete: () -> Unit) {
    val zone = ZoneId.systemDefault()
    var name by remember { mutableStateOf(original?.name ?: "") }
    var kind by remember { mutableStateOf(original?.kind ?: SavingsKind.SIP) }
    var amount by remember { mutableStateOf(original?.monthlyAmount?.let { if (it > 0) it.toString().removeSuffix(".0") else "" } ?: "") }
    var day by remember { mutableStateOf((original?.dayOfMonth ?: 5).toString()) }
    var start by remember { mutableStateOf(original?.startDate ?: System.currentTimeMillis()) }
    var months by remember {
        mutableStateOf(original?.endDate?.let {
            val s = MonthKey.of(original.startDate, zone); val e = MonthKey.of(it, zone)
            ((e.year - s.year) * 12 + e.month - s.month + 1).toString()
        } ?: "")
    }
    var goal by remember { mutableStateOf(original?.targetAmount?.toString()?.removeSuffix(".0") ?: "") }
    var ret by remember { mutableStateOf(original?.expectedReturnPct?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var active by remember { mutableStateOf(original?.isActive ?: true) }
    var pickDate by remember { mutableStateOf(false) }

    val amountValue = amount.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (original == null) "New plan" else "Edit plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(name, { name = it }, label = { Text("Name (e.g. Nifty index fund)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SavingsKind.entries.forEach { k -> FilterChip(kind == k, { kind = k }, { Text(k.title) }) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Monthly ₹") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) }, label = { Text("Debit day") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Started ${Instant.ofEpochMilli(start).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}", Modifier.weight(1f))
                TextButton({ pickDate = true }) { Text("Change") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Runs for (months, blank = no end)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(goal, { goal = it }, label = { Text("Goal ₹ (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(ret, { ret = it }, label = { Text("Return % / yr") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Text("Return is only used for a rough projection. It is an estimate, not advice or a promise.",
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active", Modifier.weight(1f)); Switch(active, { active = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (original != null) TextButton(onDelete) { Text("Delete", color = Color(0xFFF87171)) }
                Box(Modifier.weight(1f))
                TextButton(onDismiss) { Text("Cancel") }
                TextButton(
                    enabled = name.isNotBlank() && amountValue > 0,
                    onClick = {
                        val end = months.toIntOrNull()?.takeIf { it > 0 }?.let {
                            Instant.ofEpochMilli(start).atZone(zone).plusMonths((it - 1).toLong()).toInstant().toEpochMilli()
                        }
                        onSave(
                            (original ?: SavingsPlan(name = name, monthlyAmount = amountValue)).copy(
                                name = name.trim(), kind = kind, monthlyAmount = amountValue,
                                dayOfMonth = (day.toIntOrNull() ?: 5).coerceIn(1, 28), startDate = start, endDate = end,
                                targetAmount = goal.toDoubleOrNull()?.takeIf { it > 0 },
                                expectedReturnPct = ret.toDoubleOrNull() ?: 0.0, isActive = active,
                            )
                        )
                    },
                ) { Text("Save") }
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = start)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton({ state.selectedDateMillis?.let { start = it }; pickDate = false }) { Text("OK") } },
            dismissButton = { TextButton({ pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}
