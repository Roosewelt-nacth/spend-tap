package com.austin.spendtap.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.austin.spendtap.R
import com.austin.spendtap.quick.QuickTileService
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: MainViewModel, pad: PaddingValues) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.exportStatus.collectAsStateWithLifecycle()
    val exporting by vm.isExporting.collectAsStateWithLifecycle()
    val s = settings ?: return          // wait for DataStore so fields don't start from wrong zeros
    val context = LocalContext.current
    val year = LocalDate.now().year

    var income by remember { mutableStateOf(s.monthlyIncome.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var budget by remember { mutableStateOf(s.monthlyBudget.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var url by remember { mutableStateOf(s.sheetsUrl) }
    var token by remember { mutableStateOf(s.sheetsToken) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, pad.calculateTopPadding() + 12.dp, 16.dp, pad.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassCard {
                Text("Monthly plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(income, { income = it; vm.updateSettings { c -> c.copy(monthlyIncome = it.toDoubleOrNull() ?: 0.0) } },
                        label = { Text("Monthly income ₹") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(budget, { budget = it; vm.updateSettings { c -> c.copy(monthlyBudget = it.toDoubleOrNull() ?: 0.0) } },
                        label = { Text("Monthly spending budget ₹") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Text("Leave the budget empty and \"left to spend\" becomes income minus your SIPs and savings.",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
        item {
            GlassCard {
                Text("Quick log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Add the \"Log spend\" tile to your quick settings, then one swipe and a tap opens the keypad over any app. " +
                    "On Pixel phones you can also set Settings › System › Gestures › Quick Tap to open Spend Tap's Log spend shortcut.",
                    Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(onClick = {
                        val sbm = context.getSystemService(StatusBarManager::class.java)
                        sbm.requestAddTileService(
                            ComponentName(context, QuickTileService::class.java), "Log spend",
                            AndroidIcon.createWithResource(context, R.drawable.ic_tile),
                            ContextCompat.getMainExecutor(context), {},
                        )
                    }) { Text("Add the tile") }
                } else {
                    Text("Open your quick settings editor and drag in \"Log spend\".", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            GlassCard {
                Text("Yearly Google Sheet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(url, { url = it; vm.updateSettings { c -> c.copy(sheetsUrl = it.trim()) } },
                        label = { Text("Web app URL (…/exec)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(token, { token = it; vm.updateSettings { c -> c.copy(sheetsToken = it) } },
                        label = { Text("Secret from the script") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Button({ vm.exportYear(year) }, enabled = !exporting) { Text("Export $year now") }
                    OutlinedButton({ vm.exportYear(year - 1) }, enabled = !exporting) { Text("Export ${year - 1}") }
                    if (exporting) CircularProgressIndicator()
                    if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text("Once a year, in January, last year is uploaded automatically to a tab named after the year. " +
                        "Setup steps are at the top of google-sheets/Code.gs in the project.",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
        item {
            GlassCard {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Your data stays on this phone. The only thing that leaves it is the yearly report, sent to the Google Sheet link you set above.",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
