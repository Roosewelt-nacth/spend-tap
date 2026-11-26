package com.austin.spendtap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    Month("Month", Icons.Rounded.CalendarMonth),
    Analytics("Analytics", Icons.Rounded.PieChart),
    Savings("Savings", Icons.Rounded.Savings),
    Settings("Settings", Icons.Rounded.Settings),
}

@Composable
fun SpendTapRoot(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color.Black.copy(alpha = 0.35f)) {
                    Tab.entries.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.White.copy(alpha = 0.18f)),
                        )
                    }
                }
            },
        ) { pad ->
            when (Tab.entries[tab]) {
                Tab.Month -> MonthScreen(vm, pad)
                Tab.Analytics -> AnalyticsScreen(vm, pad)
                Tab.Savings -> SavingsScreen(vm, pad)
                Tab.Settings -> SettingsScreen(vm, pad)
            }
        }
    }
}
