package com.austin.spendtap.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.austin.spendtap.model.CategoryTotal
import com.austin.spendtap.model.MonthKey
import java.time.Month
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.max

@Composable
fun AnalyticsScreen(vm: MainViewModel, pad: PaddingValues) {
    val monthSummary by vm.summary.collectAsStateWithLifecycle()
    val yearSummary by vm.yearSummary.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var yearScope by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }

    val totals = if (yearScope) yearSummary.byCategory else monthSummary.byCategory
    val grand = totals.sumOf { it.total }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, pad.calculateTopPadding() + 12.dp, 16.dp, pad.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!yearScope, { yearScope = false; selected = null }, { Text("Month") })
                FilterChip(yearScope, { yearScope = true; selected = null }, { Text("Year") })
            }
        }
        item {
            val step = if (yearScope) 12 else 1
            PeriodSwitcher(
                title = if (yearScope) "${month.year}" else monthTitle(month),
                canNext = month.adding(step) <= MonthKey.now(),
                onPrev = { vm.shiftMonth(-step); selected = null },
                onNext = { vm.shiftMonth(step); selected = null },
            )
        }
        item { FilterBar(filter, vm::setFilter) }
        item {
            GlassCard(corner = 32.dp) {
                if (totals.isEmpty()) {
                    Text("Nothing to chart yet.", Modifier.fillMaxWidth().height(240.dp).padding(top = 100.dp),
                        color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                } else {
                    Donut(totals, selected, onSelect = { selected = it }, centerLabel = if (yearScope) "THIS YEAR" else "THIS MONTH", grand = grand)
                }
            }
        }
        item {
            GlassCard {
                Eyebrow(if (yearScope) "MONTHLY SPEND" else "DAILY SPEND")
                if (yearScope) {
                    Bars(yearSummary.byMonth, listOf(Color(0xFF6EE7B7), Color(0xFF14B8A6)))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Month.entries.forEach {
                            Text(it.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                } else {
                    Bars(monthSummary.byDay.map { it.total }, listOf(Color(0xFF22D3EE), Color(0xFF4F46E5)))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("1", "8", "15", "22", "${monthSummary.key.daysInMonth()}").forEach {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        val income = settings?.monthlyIncome ?: 0.0
        if (!yearScope && income > 0) item { IncomeSplit(income, monthSummary.spentAll, monthSummary.savingsCommitted) }
        item { Legend(totals, selected) { selected = if (selected == it) null else it } }
    }
}

@Composable
private fun Donut(totals: List<CategoryTotal>, selected: String?, onSelect: (String?) -> Unit, centerLabel: String, grand: Double) {
    val sweep = remember(totals) { Animatable(0f) }
    LaunchedEffect(totals) { sweep.animateTo(1f, tween(900)) }

    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(250.dp).pointerInput(totals) {
                detectTapGestures { tap ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    var deg = Math.toDegrees(atan2((tap.y - c.y).toDouble(), (tap.x - c.x).toDouble())) + 90.0
                    if (deg < 0) deg += 360.0
                    val fraction = deg / 360.0
                    var run = 0.0
                    for (t in totals) {
                        run += t.share
                        if (fraction <= run) { onSelect(if (selected == t.category) null else t.category); return@detectTapGestures }
                    }
                    onSelect(null)
                }
            },
        ) {
            val stroke = 38.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var start = -90f
            for (t in totals) {
                val full = (t.share * 360f).toFloat()
                val drawn = max(0f, full * sweep.value - 2f)
                val alpha = if (selected == null || selected == t.category) 1f else 0.28f
                drawArc(SpendCategory.of(t.category).color.copy(alpha = alpha), start + 1f, drawn, false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                start += full
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(130.dp), verticalArrangement = Arrangement.Center) {
            val sel = totals.firstOrNull { it.category == selected }
            if (sel != null) {
                val cat = SpendCategory.of(sel.category)
                Icon(cat.icon, null, tint = cat.color)
                Text(cat.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(rupees(sel.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("${(sel.share * 100).toInt()}% of spend", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            } else {
                Eyebrow(centerLabel)
                AnimatedMoney(grand, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                Text("Tap a slice", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun Bars(values: List<Double>, colors: List<Color>) {
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(700)) }
    Canvas(Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp)) {
        val top = values.maxOrNull() ?: 0.0
        if (top <= 0) return@Canvas
        val slot = size.width / values.size
        val barW = slot * 0.62f
        values.forEachIndexed { i, v ->
            val h = (v / top).toFloat() * size.height * progress.value
            if (h > 0f) drawRoundRect(
                brush = Brush.verticalGradient(colors, startY = size.height - h, endY = size.height),
                topLeft = Offset(i * slot + (slot - barW) / 2, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 3),
            )
        }
    }
}

@Composable
private fun IncomeSplit(income: Double, spent: Double, saved: Double) {
    val free = max(0.0, income - spent - saved)
    GlassCard {
        Eyebrow("WHERE YOUR INCOME WENT")
        Row(Modifier.fillMaxWidth().padding(top = 12.dp).height(14.dp).clip(RoundedCornerShape(7.dp)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            fun w(v: Double) = max(0.0001f, (v / income).toFloat().coerceIn(0f, 1f))
            Box(Modifier.weight(w(spent)).fillMaxSize().background(Color(0xFFFF6B9A)))
            Box(Modifier.weight(w(saved)).fillMaxSize().background(Color(0xFF6EE7B7)))
            Box(Modifier.weight(w(free)).fillMaxSize().background(Color.White.copy(alpha = 0.25f)))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            listOf(Triple(Color(0xFFFF6B9A), "Spent", spent), Triple(Color(0xFF6EE7B7), "Saved", saved), Triple(Color.White.copy(alpha = 0.4f), "Free", free))
                .forEach { (c, label, v) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(c, CircleShape))
                        Column(Modifier.padding(start = 6.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text(rupees(v, compact = true), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
        }
    }
}

@Composable
private fun Legend(totals: List<CategoryTotal>, selected: String?, onTap: (String) -> Unit) {
    GlassCard {
        if (totals.isEmpty()) Text("No data for this period.", color = Color.White.copy(alpha = 0.6f))
        totals.forEach { t ->
            val cat = SpendCategory.of(t.category)
            Column(Modifier.fillMaxWidth().clickable { onTap(t.category) }.padding(vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(22.dp))
                    Text(cat.title, Modifier.padding(start = 10.dp), fontWeight = if (selected == t.category) FontWeight.Bold else FontWeight.Normal)
                    Text("  ${t.count}×", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
                    Box(Modifier.weight(1f))
                    Text(rupees(t.total), fontWeight = FontWeight.SemiBold)
                    Text("${(t.share * 100).toInt()}%", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                }
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                    Box(Modifier.fillMaxWidth(t.share.toFloat().coerceIn(0.01f, 1f)).fillMaxSize().background(cat.color, CircleShape))
                }
            }
        }
    }
}
