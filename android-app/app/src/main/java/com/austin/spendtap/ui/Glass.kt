package com.austin.spendtap.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.austin.spendtap.model.SpendEntry
import com.austin.spendtap.model.SpendFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * "Glass" without a blur library: translucent gradient cards with a light edge, over a
 * slowly moving colour field. Because the background is smooth, blurring it would look the
 * same, so we skip the cost and the extra dependency.
 */
@Composable
fun AuroraBackground() {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Canvas(Modifier.fillMaxSize().background(Color(0xFF050608))) {
        val w = size.width
        val h = size.height
        val p = t.value   // read in the draw phase: only redraws, never recomposes
        fun blob(color: Color, cx: Float, cy: Float, r: Float) {
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.50f), Color.Transparent), Offset(cx, cy), r),
                radius = r, center = Offset(cx, cy),
            )
        }
        blob(Color(0xFF4F46E5), w * (0.15f + 0.35f * p), h * 0.12f, w * 0.95f)
        blob(Color(0xFF14B8A6), w * (0.85f - 0.35f * p), h * (0.45f + 0.10f * p), w * 0.85f)
        blob(Color(0xFF9333EA), w * (0.20f + 0.20f * p), h * 0.85f, w * 0.90f)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.06f))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.22f))),
                shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun AnimatedMoney(value: Double, style: TextStyle, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(value.toFloat(), tween(700), label = "money")
    Text(rupees(v.toDouble()), style = style, maxLines = 1, overflow = TextOverflow.Clip, modifier = modifier)
}

@Composable
fun Eyebrow(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f), letterSpacing = 1.2.sp)
}

@Composable
fun PeriodSwitcher(title: String, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(onPrev) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous") }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        GlassIconButton(onNext, enabled = canNext) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Next") }
    }
}

@Composable
fun GlassIconButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.14f else 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Category chips + payment / impulse filters. Shared by Month and Analytics. */
@Composable
fun FilterBar(filter: SpendFilter, onChange: (SpendFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SpendCategory.entries.toList()) { cat ->
                val on = cat.key in filter.categories
                FilterChip(
                    selected = on,
                    onClick = {
                        onChange(filter.copy(categories = if (on) filter.categories - cat.key else filter.categories + cat.key))
                    },
                    label = { Text(cat.title) },
                    leadingIcon = { Icon(cat.icon, null, Modifier.size(18.dp), tint = if (on) Color.White else cat.color) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        selectedContainerColor = cat.color.copy(alpha = 0.55f),
                        labelColor = Color.White,
                        selectedLabelColor = Color.White,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = on,
                        borderColor = Color.White.copy(alpha = 0.25f),
                        selectedBorderColor = cat.color,
                    ),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var open by remember { mutableStateOf(false) }
            Box {
                AssistChip(
                    onClick = { open = true },
                    label = { Text(when (filter.paymentMethod) { "upi" -> "UPI"; "cash" -> "Cash"; else -> "UPI + Cash" }) },
                    leadingIcon = { Icon(Icons.Rounded.CreditCard, null, Modifier.size(18.dp)) },
                )
                DropdownMenu(open, { open = false }) {
                    listOf<Pair<String?, String>>(null to "UPI + Cash", "upi" to "UPI", "cash" to "Cash").forEach { (v, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { onChange(filter.copy(paymentMethod = v)); open = false })
                    }
                }
            }
            FilterChip(
                selected = filter.impulseOnly,
                onClick = { onChange(filter.copy(impulseOnly = !filter.impulseOnly)) },
                label = { Text("Impulse only") },
                leadingIcon = { Icon(Icons.Rounded.Bolt, null, Modifier.size(18.dp)) },
            )
            if (filter.isActive) TextButton({ onChange(SpendFilter()) }) { Text("Clear") }
        }
    }
}

@Composable
fun EntryRow(entry: SpendEntry, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val cat = SpendCategory.of(entry.category)
    val date = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))
    GlassCard(modifier, corner = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(30.dp))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    if (entry.isImpulse) Icon(Icons.Rounded.Bolt, "Impulse", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
                Text(
                    if (entry.note.isBlank()) date else "${entry.note} · $date",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(rupees(entry.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(entry.paymentMethod.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
            }
            IconButton(onDelete) { Icon(Icons.Rounded.Delete, "Delete", tint = Color.White.copy(alpha = 0.55f)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(selected: SpendCategory?, onSelect: (SpendCategory) -> Unit, items: List<SpendCategory> = SpendCategory.entries) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat.title) },
                leadingIcon = { Icon(cat.icon, null, Modifier.size(18.dp), tint = if (selected == cat) Color.White else cat.color) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    selectedContainerColor = cat.color.copy(alpha = 0.55f),
                    labelColor = Color.White, selectedLabelColor = Color.White,
                ),
            )
        }
    }
}
