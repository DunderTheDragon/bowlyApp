package com.cantbebetter.bowly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cantbebetter.bowly.data.network.ConsumedMealDto
import com.cantbebetter.bowly.data.network.DailySummaryDto
import com.cantbebetter.bowly.ui.components.NutritionPanel
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

private val ProteinColor = Color(0xFFD81B60)
private val FatColor = Color(0xFFF57C00)
private val CarbsColor = Color(0xFF388E3C)

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onAddMealClick: (String) -> Unit,
    onEditMealClick: (ConsumedMealDto) -> Unit,
    onDeleteMealClick: (ConsumedMealDto) -> Unit
) {
    var currentDayOffset by remember { mutableStateOf(0) }
    val todayMillis = Clock.getTodayMillis()
    val dayStart = (todayMillis + (currentDayOffset * 86400000L))
    val dateString = Clock.formatToApiDate(dayStart)
    
    val dailySummary by viewModel.dailySummary.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val dailyStats by remember(userProfile) { 
        derivedStateOf { userProfile?.let { viewModel.calculateDailyStats(it) } } 
    }
    
    val mealNames = listOf("Śniadanie", "Drugie śniadanie", "Obiad", "Podwieczorek", "Kolacja")

    LaunchedEffect(currentDayOffset) {
        viewModel.loadDailySummary(dateString)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val summary = dailySummary

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            item {
                DaySelector(
                    offset = currentDayOffset,
                    onOffsetChange = { currentDayOffset = it }
                )
            }

            item {
                NutritionPanel(
                    calories = summary?.totalCalories ?: 0.0,
                    tCalories = dailyStats?.targetCalories ?: 0.0,
                    protein = summary?.totalProtein ?: 0.0,
                    tProtein = dailyStats?.targetProtein ?: 0.0,
                    fat = summary?.totalFat ?: 0.0,
                    tFat = dailyStats?.targetFat ?: 0.0,
                    carbs = summary?.totalCarbs ?: 0.0,
                    tCarbs = dailyStats?.targetCarbs ?: 0.0
                )
            }

            mealNames.forEach { mealName ->
                val meals = summary?.meals?.get(mealName) ?: emptyList()
                val totalCals = meals.sumOf { it.calories }
                val totalProtein = meals.sumOf { it.protein }
                val totalFat = meals.sumOf { it.fat }
                val totalCarbs = meals.sumOf { it.carbs }

                item {
                    MealHeader(
                        name = mealName,
                        calories = totalCals,
                        protein = totalProtein,
                        fat = totalFat,
                        carbs = totalCarbs,
                        onAddClick = { onAddMealClick(mealName) }
                    )
                }

                if (meals.isEmpty()) {
                    item {
                        Text(
                            "Brak wpisów",
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    items(meals) { meal ->
                        MealItem(
                            meal,
                            onEditClick = { onEditMealClick(meal) },
                            onDeleteClick = { onDeleteMealClick(meal) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DaySelector(offset: Int, onOffsetChange: (Int) -> Unit) {
    val dateText = when (offset) {
        0 -> "Dzisiaj"
        -1 -> "Wczoraj"
        1 -> "Jutro"
        else -> {
            if (offset < 0) "${-offset} dni temu"
            else "Za $offset dni"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onOffsetChange(offset - 1) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Poprzedni dzień")
        }
        
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        IconButton(onClick = { onOffsetChange(offset + 1) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Następny dzień")
        }
    }
}

@Composable
fun MealItem(
    meal: ConsumedMealDto,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(meal.segmentName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${meal.weightG.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${meal.calories.toInt()} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Więcej")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edytuj") },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Usuń") },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) },
                                colors = MenuDefaults.itemColors(
                                    leadingIconColor = MaterialTheme.colorScheme.error,
                                    textColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("B: ${meal.protein.toInt()}g", style = MaterialTheme.typography.labelSmall, color = ProteinColor)
                        Text("T: ${meal.fat.toInt()}g", style = MaterialTheme.typography.labelSmall, color = FatColor)
                        Text("W: ${meal.carbs.toInt()}g", style = MaterialTheme.typography.labelSmall, color = CarbsColor)
                    }
                }
            }
        }
    }
}

@Composable
fun MealDetailDialog(meal: ConsumedMealDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(meal.segmentName) },
        text = {
            Column {
                Text("Waga: ${meal.weightG.toInt()}g", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MacroDetail("Białko", meal.protein, ProteinColor)
                    MacroDetail("Tłuszcze", meal.fat, FatColor)
                    MacroDetail("Węgle", meal.carbs, CarbsColor)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Łącznie: ${meal.calories.toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zamknij") }
        }
    )
}

@Composable
fun MealHeader(
    name: String,
    calories: Double,
    protein: Double,
    fat: Double,
    carbs: Double,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("B: ${protein.toInt()}g", style = MaterialTheme.typography.labelSmall, color = ProteinColor)
                    Text("T: ${fat.toInt()}g", style = MaterialTheme.typography.labelSmall, color = FatColor)
                    Text("W: ${carbs.toInt()}g", style = MaterialTheme.typography.labelSmall, color = CarbsColor)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${calories.toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onAddClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Dodaj do $name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun MacroDetail(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${value.toInt()}g",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
