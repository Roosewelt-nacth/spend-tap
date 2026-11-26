package com.austin.spendtap.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.austin.spendtap.model.MonthKey
import com.austin.spendtap.model.MonthSummary
import com.austin.spendtap.model.SpendEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

fun monthTitle(m: MonthKey): String =
    YearMonth.of(m.year, m.month).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))

@Composable
fun MonthScreen(vm: MainViewModel, pad: PaddingValues) {
    val s by vm.summary.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 12.dp,
                bottom = pad.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PeriodSwitcher(monthTitle(month), canNext = month.adding(1) <= MonthKey.now(),
                    onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
            }
            item { HeroCard(s) }
            item { FilterBar(filter, vm::setFilter) }
            item { StatsRow(s) }
            if (s.entries.isEmpty()) {
                item {
                    Text(
                        if (filter.isActive) "Nothing matches these filters." else "No spends this month yet.\nUse the Log spend tile or the + button.",
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            items(s.entries, key = { it.id }) { e ->
                EntryRow(e, onDelete = { vm.delete(e) }, modifier = Modifier.animateItem())
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = pad.calculateBottomPadding() + 20.dp),
        ) { Icon(Icons.Rounded.Add, "Add spend") }
    }

    if (showAdd) AddEntrySheet(onDismiss = { showAdd = false }, onSave = { vm.add(it); showAdd = false })
}

@Composable
private fun HeroCard(s: MonthSummary) {
    val tint = when {
        s.progress >= 1.0 -> Color(0xFFF87171)
        s.progress >= 0.75 -> Color(0xFFFFB74D)
        else -> Color(0xFF6EE7B7)
    }
    val ring by animateFloatAsState(minOf(1.0, s.progress).toFloat(), tween(900), label = "ring")

    GlassCard(corner = 32.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(112.dp)) {
                    val stroke = 14.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke))
                    drawArc(tint, -90f, 360f * ring, false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text(if (s.limit == null) "—" else "${minOf(s.progress, 9.99).times(100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.padding(start = 18.dp)) {
                Eyebrow("SPENT THIS MONTH")
                AnimatedMoney(s.spentAll, MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 32.sp))
                val rem = s.remaining
                if (rem != null) {
                    Text(
                        if (rem >= 0) "${rupees(rem)} left" else "${rupees(-rem)} over budget",
                        color = if (rem >= 0) Color(0xFF6EE7B7) else Color(0xFFF87171),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text("Set income or a budget in Settings", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
                if (s.savingsCommitted > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Rounded.Eco, null, Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.6f))
                        Text("  ${rupees(s.savingsCommitted)} set aside for SIPs",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(s: MonthSummary) {
    val today = LocalDate.now()
    val isCurrent = s.key == MonthKey.now()
    val divisor = if (isCurrent) maxOf(1, today.dayOfMonth) else s.key.daysInMonth()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Entries", "${s.count}", Icons.Rounded.ListAlt, Modifier.weight(1f))
        StatTile("Per day", rupees(s.spent / divisor, compact = true), Icons.Rounded.WbSunny, Modifier.weight(1f))
        StatTile("Impulse", rupees(s.impulse, compact = true), Icons.Rounded.Bolt, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    GlassCard(modifier, corner = 22.dp) {
        Icon(icon, null, Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(onDismiss: () -> Unit, onSave: (SpendEntry) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SpendCategory.Personal) }
    var note by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("upi") }
    var impulse by remember { mutableStateOf(false) }
    var yesterday by remember { mutableStateOf(false) }
    val value = amount.replace(",", "").toDoubleOrNull() ?: 0.0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Add spend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                amount, { amount = it }, label = { Text("Amount (₹)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
            CategoryChips(category, { category = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!yesterday, { yesterday = false }, { Text("Today") })
                FilterChip(yesterday, { yesterday = true }, { Text("Yesterday") })
                Box(Modifier.weight(1f))
                FilterChip(payment == "upi", { payment = "upi" }, { Text("UPI") })
                FilterChip(payment == "cash", { payment = "cash" }, { Text("Cash") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Impulse buy", Modifier.weight(1f))
                Switch(impulse, { impulse = it })
            }
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val whenMs = if (yesterday) now - 24L * 60 * 60 * 1000 else now
                        onSave(SpendEntry(amount = value, category = category.key, note = note.trim(),
                            paymentMethod = payment, isImpulse = impulse, createdAt = whenMs))
                    },
                    enabled = value > 0,
                ) { Text("Save") }
            }
        }
    }
}
