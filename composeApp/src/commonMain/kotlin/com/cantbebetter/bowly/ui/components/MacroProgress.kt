package com.cantbebetter.bowly.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NutritionPanel(
    calories: Double, tCalories: Double,
    bonusCalories: Double = 0.0,
    protein: Double, tProtein: Double,
    fat: Double, tFat: Double,
    carbs: Double, tCarbs: Double
) {
    val effectiveTarget = (tCalories + bonusCalories).coerceAtLeast(1.0)
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Lewa strona: Kalorie
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                CircularProgressIndicator(
                    progress = { (calories / effectiveTarget).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${calories.toInt()}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    )
                    Text(
                        text = "kcal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (bonusCalories > 0) {
                            "cel: ${effectiveTarget.toInt()} (+${bonusCalories.toInt()})"
                        } else {
                            "cel: ${effectiveTarget.toInt()}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Prawa strona: Makroskładniki
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Szczyt trójkąta: Białko
                MacroCircularItem(
                    label = "Białko",
                    current = protein,
                    target = tProtein,
                    color = Color(0xFFD81B60) // Ciemniejszy róż
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Podstawa lewo: Tłuszcze
                    MacroCircularItem(
                        label = "Tłuszcze",
                        current = fat,
                        target = tFat,
                        color = Color(0xFFF57C00) // Pomarańczowy
                    )
                    // Podstawa prawo: Węgle
                    MacroCircularItem(
                        label = "Węgle",
                        current = carbs,
                        target = tCarbs,
                        color = Color(0xFF388E3C) // Ciemniejszy zielony
                    )
                }
            }
        }
    }
}

@Composable
fun MacroCircularItem(label: String, current: Double, target: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
            CircularProgressIndicator(
                progress = { (current / target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 5.dp,
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round
            )
            Text(
                text = "${current.toInt()}g",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CalorieProgress(current: Double, target: Double) {
    // Zachowujemy dla kompatybilności wstecznej jeśli potrzeba, 
    // ale docelowo DashboardScreen będzie używał NutritionPanel
}

@Composable
fun MacroRow(protein: Double, tProtein: Double, fat: Double, tFat: Double, carbs: Double, tCarbs: Double) {
    // Zachowujemy dla kompatybilności
}
