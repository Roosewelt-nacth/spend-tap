package com.austin.spendtap.quick

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.austin.spendtap.SpendApp
import com.austin.spendtap.model.SpendEntry
import com.austin.spendtap.ui.CategoryChips
import com.austin.spendtap.ui.SpendCategory
import com.austin.spendtap.ui.SpendTapTheme
import com.austin.spendtap.ui.rupees
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The Android answer to Back Tap: a translucent screen that floats a keypad over whatever app
 * you were in. Type the amount, tap a category, done. Opened by the Quick Settings tile,
 * the launcher shortcut, or Pixel's Quick Tap gesture.
 */
class QuickLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as SpendApp).repo
        setContent {
            SpendTapTheme {
                var ordered by remember { mutableStateOf(SpendCategory.entries.toList()) }
                // Most-used categories first, so the common ones are one tap away.
                LaunchedEffect(Unit) {
                    val counts = repo.entries.first().groupingBy { it.category }.eachCount()
                    ordered = SpendCategory.entries.sortedByDescending { counts[it.key] ?: 0 }
                }
                QuickLogSheet(
                    categories = ordered,
                    onClose = { finish() },
                    onSave = { amount, cat ->
                        lifecycleScope.launch {
                            repo.add(SpendEntry(amount = amount, category = cat.key))
                            finish()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickLogSheet(categories: List<SpendCategory>, onClose: () -> Unit, onSave: (Double, SpendCategory) -> Unit) {
    var digits by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    val amount = digits.toDoubleOrNull() ?: 0.0

    fun press(k: String) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        digits = when {
            k == "⌫" -> digits.dropLast(1)
            k == "." -> if ('.' in digits) digits else (digits.ifEmpty { "0" } + ".")
            digits.length >= 9 -> digits
            digits == "0" -> k
            else -> digits + k
        }
        // at most two decimals
        val dot = digits.indexOf('.')
        if (dot >= 0 && digits.length - dot > 3) digits = digits.dropLast(1)
    }

    Box(
        Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xF2181B22), Color(0xF20D0F14))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.04f))), shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).height(4.dp).fillMaxWidth(0.12f).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)))
            Text("HOW MUCH WAS SPENT?", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f), letterSpacing = 1.2.sp)
            Text(
                if (digits.isEmpty()) "₹0" else "₹$digits",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold,
                color = if (digits.isEmpty()) Color.White.copy(alpha = 0.35f) else Color.White,
            )

            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.09f)).clickable { press(k) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (k == "⌫") Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete")
                            else Text(k, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }

            Text(
                if (amount > 0) "TAP A CATEGORY TO LOG ${rupees(amount).uppercase()}" else "ENTER AN AMOUNT, THEN PICK A CATEGORY",
                style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f), letterSpacing = 0.8.sp,
            )
            CategoryChips(
                selected = null,
                items = if (expanded) categories else categories.take(6),
                onSelect = { cat ->
                    if (amount > 0) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onSave(amount, cat)
                    }
                },
            )
            TextButton({ expanded = !expanded }) { Text(if (expanded) "Fewer" else "More categories") }
        }
    }
}
