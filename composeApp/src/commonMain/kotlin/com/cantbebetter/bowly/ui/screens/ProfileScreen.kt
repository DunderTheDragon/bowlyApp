package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.MockData
import com.cantbebetter.bowly.models.AllAvailableMealTypes
import com.cantbebetter.bowly.models.MacroRatios
import com.cantbebetter.bowly.models.User
import kotlin.math.roundToInt

@Composable
fun ProfileScreen() {
    val user = MockData.currentUser
    val scrollState = rememberScrollState()

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
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(24.dp))

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
                    onCheckedChange = { MockData.updateCurrentUser(user.copy(isDarkTheme = it)) }
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
                        onValueChange = { val v = it.toDoubleOrNull(); if (v != null) MockData.updateCurrentUser(user.copy(weightKg = v)) },
                        label = { Text("Waga (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = user.targetWeightKg.toString(),
                        onValueChange = { val v = it.toDoubleOrNull(); if (v != null) MockData.updateCurrentUser(user.copy(targetWeightKg = v)) },
                        label = { Text("Cel (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Tempo zmiany: ${user.weeklyChangeRateKg} kg / tydzień", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = user.weeklyChangeRateKg.toFloat(),
                    onValueChange = { MockData.updateCurrentUser(user.copy(weeklyChangeRateKg = (it * 10).roundToInt() / 10.0)) },
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
                Text("Suma: ${user.macroRatios.protein + user.macroRatios.fat + user.macroRatios.carbs}%", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (user.macroRatios.protein + user.macroRatios.fat + user.macroRatios.carbs == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                MacroSlider("Białko", user.macroRatios.protein) { newVal ->
                    updateMacros(user, newVal, "protein")
                }
                
                MacroSlider("Tłuszcze", user.macroRatios.fat) { newVal ->
                    updateMacros(user, newVal, "fat")
                }
                
                MacroSlider("Węglowodany", user.macroRatios.carbs) { newVal ->
                    updateMacros(user, newVal, "carbs")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Aktywne posiłki
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Aktywne Posiłki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Zmiany dotyczą dzisiejszego i przyszłych dni.", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(12.dp))
                
                val currentTypes = MockData.getMealTypesForDate(Clock.now())
                AllAvailableMealTypes.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val newTypes = if (currentTypes.contains(type)) {
                                if (currentTypes.size > 1) currentTypes - type else currentTypes
                            } else {
                                (currentTypes + type).sortedBy { AllAvailableMealTypes.indexOf(it) }
                            }
                            MockData.updateMealTypesFromToday(newTypes)
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentTypes.contains(type),
                            onCheckedChange = null // Handled by row clickable
                        )
                        Text(type, style = MaterialTheme.typography.bodyMedium)
                    }
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
                Text("Kalorie: ${MockData.dailyStats.targetCalories.toInt()} kcal", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("B: ${MockData.dailyStats.targetProtein.toInt()}g")
                    Text("T: ${MockData.dailyStats.targetFat.toInt()}g")
                    Text("W: ${MockData.dailyStats.targetCarbs.toInt()}g")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(
            onClick = { /* Logout */ },
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

private fun updateMacros(user: User, newValue: Int, changedField: String) {
    val current = user.macroRatios
    val oldVal = when(changedField) {
        "protein" -> current.protein
        "fat" -> current.fat
        "carbs" -> current.carbs
        else -> 0
    }
    
    val diff = newValue - oldVal
    if (diff == 0) return

    val others = mutableListOf<Pair<String, Int>>()
    if (changedField != "protein") others.add("protein" to current.protein)
    if (changedField != "fat") others.add("fat" to current.fat)
    if (changedField != "carbs") others.add("carbs" to current.carbs)

    val sumOthers = others.sumOf { it.second }
    
    val newMacroRatios = if (sumOthers == 0) {
        // Jeśli pozostałe są 0, rozdzielamy różnicę po równo
        val shared = (100 - newValue) / 2
        val remainder = (100 - newValue) % 2
        when(changedField) {
            "protein" -> MacroRatios(newValue, shared, shared + remainder)
            "fat" -> MacroRatios(shared, newValue, shared + remainder)
            else -> MacroRatios(shared, shared + remainder, newValue)
        }
    } else {
        // Zachowujemy proporcje
        val factor = (sumOthers - diff).toDouble() / sumOthers
        var newOthers = others.map { it.first to (it.second * factor).roundToInt().coerceAtLeast(0) }.toMap().toMutableMap()
        
        // Korekta aby suma była 100
        var currentSum = newValue + newOthers.values.sum()
        while (currentSum != 100) {
            val adj = if (currentSum < 100) 1 else -1
            // Szukamy największego/najmniejszego aby skorygować
            val keyToAdjust = if (adj > 0) {
                newOthers.maxBy { it.value }.key
            } else {
                newOthers.filter { it.value > 0 }.maxBy { it.value }.key
            }
            newOthers[keyToAdjust] = newOthers[keyToAdjust]!! + adj
            currentSum = newValue + newOthers.values.sum()
        }

        when(changedField) {
            "protein" -> MacroRatios(newValue, newOthers["fat"]!!, newOthers["carbs"]!!)
            "fat" -> MacroRatios(newOthers["protein"]!!, newValue, newOthers["carbs"]!!)
            else -> MacroRatios(newOthers["protein"]!!, newOthers["fat"]!!, newValue)
        }
    }

    MockData.updateCurrentUser(user.copy(macroRatios = newMacroRatios))
}
