package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.network.UserDto
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel
import kotlin.math.roundToInt

private val activityLevels = listOf(
    1.2 to "Siedzący tryb",
    1.375 to "Lekka aktywność",
    1.55 to "Umiarkowana aktywność",
    1.725 to "Wysoka aktywność"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onMyProductsClick: () -> Unit,
    onAdminPanelClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val scrollState = rememberScrollState()

    val user = userProfile ?: return
    val isSystemDark = isSystemInDarkTheme()
    val effectiveIsDark = user.isDarkTheme ?: isSystemDark
    val macroDisplay = remember(user.proteinRatio, user.fatRatio, user.carbsRatio) {
        user.macroValues().toDisplay()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    user.username.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(user.username, style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        if (user.role == "ADMIN") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onAdminPanelClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Panel Administratora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onMyProductsClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.List, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Moje produkty i przepisy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (effectiveIsDark) Icons.Default.DarkMode else Icons.Default.LightMode, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Motyw ciemny")
                }
                Switch(
                    checked = effectiveIsDark,
                    onCheckedChange = { viewModel.updateUserProfile(user.copy(isDarkTheme = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Parametry i cel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Płeć", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = user.gender == "MALE",
                        onClick = { viewModel.updateUserProfile(user.copy(gender = "MALE")) },
                        label = { Text("Mężczyzna") }
                    )
                    FilterChip(
                        selected = user.gender == "FEMALE",
                        onClick = { viewModel.updateUserProfile(user.copy(gender = "FEMALE")) },
                        label = { Text("Kobieta") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = user.age.toString(),
                        onValueChange = { it.toIntOrNull()?.let { age -> viewModel.updateUserProfile(user.copy(age = age)) } },
                        label = { Text("Wiek") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = user.heightCm.toString().removeSuffix(".0"),
                        onValueChange = { it.toDoubleOrNull()?.let { h -> viewModel.updateUserProfile(user.copy(heightCm = h)) } },
                        label = { Text("Wzrost (cm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = user.weightKg.toString().removeSuffix(".0"),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateUserProfile(user.copy(weightKg = v)) } },
                        label = { Text("Waga (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = user.targetWeightKg.toString().removeSuffix(".0"),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateUserProfile(user.copy(targetWeightKg = v)) } },
                        label = { Text("Cel (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Tempo zmiany: ${user.weeklyChangeRateKg} kg / tydzień", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = user.weeklyChangeRateKg.toFloat(),
                    onValueChange = {
                        viewModel.updateUserProfile(
                            user.copy(weeklyChangeRateKg = (it * 10).roundToInt() / 10.0)
                        )
                    },
                    valueRange = 0.1f..1.0f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(8.dp))

                var activityExpanded by remember { mutableStateOf(false) }
                val activityLabel = activityLevels.find { kotlin.math.abs(it.first - user.activityLevel) < 0.001 }?.second
                    ?: "Aktywność: ${user.activityLevel}"

                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = activityLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Poziom aktywności") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = activityExpanded,
                        onDismissRequest = { activityExpanded = false }
                    ) {
                        activityLevels.forEach { (level, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateUserProfile(user.copy(activityLevel = level))
                                    activityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Proporcje makroskładników", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Suma: ${macroDisplay.sum}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                MacroSlider("Białko", macroDisplay.protein) { newVal ->
                    val adjusted = adjustMacroRatios(user.macroValues(), MacroField.PROTEIN, newVal)
                    viewModel.updateUserProfile(adjusted.applyTo(user))
                }

                MacroSlider("Tłuszcze", macroDisplay.fat) { newVal ->
                    val adjusted = adjustMacroRatios(user.macroValues(), MacroField.FAT, newVal)
                    viewModel.updateUserProfile(adjusted.applyTo(user))
                }

                MacroSlider("Węglowodany", macroDisplay.carbs) { newVal ->
                    val adjusted = adjustMacroRatios(user.macroValues(), MacroField.CARBS, newVal)
                    viewModel.updateUserProfile(adjusted.applyTo(user))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Twoje zapotrzebowanie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                dailyStats?.let { stats ->
                    Text(
                        "Kalorie: ${stats.targetCalories.toInt()} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("B: ${stats.targetProtein.toInt()}g")
                        Text("T: ${stats.targetFat.toInt()}g")
                        Text("W: ${stats.targetCarbs.toInt()}g")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(
            onClick = onLogoutClick,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Wyloguj się")
        }
    }
}

@Composable
fun MacroSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value%", fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 100)) },
            valueRange = 0f..100f,
            steps = 99
        )
    }
}
