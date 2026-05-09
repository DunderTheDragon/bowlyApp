package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.network.UserDto
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel
import kotlin.math.roundToInt

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

    val user = userProfile ?: return // Wait for profile to load

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

        // Admin Panel Button
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

        // Moje Produkty Button
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
                    Icon(Icons.Default.List, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Moje produkty i przepisy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Motyw
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (user.isDarkTheme == true) Icons.Default.DarkMode else Icons.Default.LightMode, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Motyw ciemny")
                }
                Switch(
                    checked = user.isDarkTheme == true,
                    onCheckedChange = { viewModel.updateUserProfile(user.copy(isDarkTheme = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Parametry ciała
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Parametry i Cel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = user.weightKg.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateUserProfile(user.copy(weightKg = v)) } },
                        label = { Text("Waga (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = user.targetWeightKg.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateUserProfile(user.copy(targetWeightKg = v)) } },
                        label = { Text("Cel (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Tempo zmiany: ${user.weeklyChangeRateKg} kg / tydzień", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = user.weeklyChangeRateKg.toFloat(),
                    onValueChange = { viewModel.updateUserProfile(user.copy(weeklyChangeRateKg = (it * 10).roundToInt() / 10.0)) },
                    valueRange = 0.1f..1.0f,
                    steps = 8
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Makroskładniki
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Proporcje Makroskładników", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val sum = user.proteinRatio + user.fatRatio + user.carbsRatio
                Text("Suma: $sum%", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (sum == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                MacroSlider("Białko", user.proteinRatio) { newVal ->
                    updateMacros(user, newVal, "protein") { viewModel.updateUserProfile(it) }
                }
                
                MacroSlider("Tłuszcze", user.fatRatio) { newVal ->
                    updateMacros(user, newVal, "fat") { viewModel.updateUserProfile(it) }
                }
                
                MacroSlider("Węglowodany", user.carbsRatio) { newVal ->
                    updateMacros(user, newVal, "carbs") { viewModel.updateUserProfile(it) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wynikowe zapotrzebowanie
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Twoje Zapotrzebowanie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                dailyStats?.let { stats ->
                    Text("Kalorie: ${stats.targetCalories.toInt()} kcal", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f
        )
    }
}

private fun updateMacros(user: UserDto, newValue: Int, changedField: String, onUpdate: (UserDto) -> Unit) {
    val oldVal = when(changedField) {
        "protein" -> user.proteinRatio
        "fat" -> user.fatRatio
        "carbs" -> user.carbsRatio
        else -> 0
    }
    
    val diff = newValue - oldVal
    if (diff == 0) return

    val others = mutableListOf<Pair<String, Int>>()
    if (changedField != "protein") others.add("protein" to user.proteinRatio)
    if (changedField != "fat") others.add("fat" to user.fatRatio)
    if (changedField != "carbs") others.add("carbs" to user.carbsRatio)

    val sumOthers = others.sumOf { it.second }
    
    val newUser = if (sumOthers == 0) {
        val shared = (100 - newValue) / 2
        val remainder = (100 - newValue) % 2
        when(changedField) {
            "protein" -> user.copy(proteinRatio = newValue, fatRatio = shared, carbsRatio = shared + remainder)
            "fat" -> user.copy(proteinRatio = shared, fatRatio = newValue, carbsRatio = shared + remainder)
            else -> user.copy(proteinRatio = shared, fatRatio = shared + remainder, carbsRatio = newValue)
        }
    } else {
        val factor = (sumOthers - diff).toDouble() / sumOthers
        var newOthers = others.map { it.first to (it.second * factor).roundToInt().coerceAtLeast(0) }.toMap().toMutableMap()
        
        var currentSum = newValue + newOthers.values.sum()
        while (currentSum != 100) {
            val adj = if (currentSum < 100) 1 else -1
            val keyToAdjust = if (adj > 0) {
                newOthers.maxBy { it.value }.key
            } else {
                newOthers.filter { it.value > 0 }.maxBy { it.value }.key
            }
            newOthers[keyToAdjust] = newOthers[keyToAdjust]!! + adj
            currentSum = newValue + newOthers.values.sum()
        }

        when(changedField) {
            "protein" -> user.copy(proteinRatio = newValue, fatRatio = newOthers["fat"]!!, carbsRatio = newOthers["carbs"]!!)
            "fat" -> user.copy(proteinRatio = newOthers["protein"]!!, fatRatio = newValue, carbsRatio = newOthers["carbs"]!!)
            else -> user.copy(proteinRatio = newOthers["protein"]!!, fatRatio = newOthers["fat"]!!, carbsRatio = newValue)
        }
    }

    onUpdate(newUser)
}
