package com.austin.spendtap.ui

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.util.Locale

/** Same keys as the iOS app and the Supabase `category` column. */
enum class SpendCategory(val key: String, val title: String, val icon: ImageVector, val color: Color) {
    Family("family", "Family", Icons.Rounded.Home, Color(0xFFFF6B9A)),
    Friends("friends", "Friends", Icons.Rounded.Groups, Color(0xFFFF9F43)),
    Personal("personal", "Personal", Icons.Rounded.Person, Color(0xFFFFD93D)),
    Petrol("petrol", "Petrol", Icons.Rounded.LocalGasStation, Color(0xFF6EE7B7)),
    Food("food", "Food & Dining", Icons.Rounded.Restaurant, Color(0xFF4ADE80)),
    Groceries("groceries", "Groceries", Icons.Rounded.ShoppingCart, Color(0xFF2DD4BF)),
    Transport("transport", "Transport", Icons.Rounded.DirectionsBus, Color(0xFF22D3EE)),
    Shopping("shopping", "Shopping", Icons.Rounded.ShoppingBag, Color(0xFF60A5FA)),
    Bills("bills", "Bills & Rent", Icons.Rounded.Receipt, Color(0xFF818CF8)),
    Health("health", "Health", Icons.Rounded.MedicalServices, Color(0xFFC084FC)),
    Entertainment("entertainment", "Entertainment", Icons.Rounded.Movie, Color(0xFFF87171)),
    Other("other", "Other", Icons.Rounded.MoreHoriz, Color(0xFF9CA3AF));

    companion object {
        fun of(key: String): SpendCategory = entries.firstOrNull { it.key == key } ?: Other
    }
}

private val FallbackDark = darkColorScheme(
    primary = Color(0xFF7FD6C2),
    secondary = Color(0xFFA8B4FF),
    tertiary = Color(0xFFFFB4D0),
    background = Color(0xFF0B0D12),
    surface = Color(0xFF14161B),
)

@Composable
fun SpendTapTheme(content: @Composable () -> Unit) {
    // Material You: pick up the user's wallpaper colours on Android 12+.
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(LocalContext.current) else FallbackDark
    MaterialTheme(colorScheme = scheme, content = content)
}

private val money: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
}

fun rupees(v: Double, compact: Boolean = false): String {
    if (compact && kotlin.math.abs(v) >= 1000) {
        return "₹" + NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 1 }.format(v / 1000) + "k"
    }
    return "₹" + money.format(v)
}
