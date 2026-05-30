package com.cantbebetter.bowly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cantbebetter.bowly.data.network.ConsumedPortionDto
import com.cantbebetter.bowly.data.network.WorkoutActivityDto
import com.cantbebetter.bowly.models.MealTypeMapper
import com.cantbebetter.bowly.ui.components.NutritionPanel
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

private val ProteinColor = Color(0xFFD81B60)
private val FatColor = Color(0xFFF57C00)
private val CarbsColor = Color(0xFF388E3C)

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    dayOffset: Int,
    onDayOffsetChange: (Int) -> Unit,
    onAddMealClick: (String, String) -> Unit,
    onEditMealClick: (ConsumedPortionDto) -> Unit
) {
    val todayMillis = Clock.getTodayMillis()
    val dayStart = (todayMillis + (dayOffset * 86400000L))
    val dateString = Clock.formatToApiDate(dayStart)
    
    val dailySummary by viewModel.dailySummary.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val dailyStats by remember(userProfile) { 
        derivedStateOf { userProfile?.let { viewModel.calculateDailyStats(it) } } 
    }
    
    val mealNames = listOf("Śniadanie", "Drugie śniadanie", "Obiad", "Podwieczorek", "Kolacja")

    var showAddWorkoutDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<WorkoutActivityDto?>(null) }

    LaunchedEffect(dayOffset) {
        viewModel.loadUserProfile()
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
                    offset = dayOffset,
                    onOffsetChange = onDayOffsetChange
                )
            }

            item {
                val burnedBonus = summary?.burnedKcal ?: 0.0
                NutritionPanel(
                    calories = summary?.totalKcal ?: 0.0,
                    tCalories = dailyStats?.targetCalories ?: 0.0,
                    bonusCalories = burnedBonus,
                    protein = summary?.totalProtein ?: 0.0,
                    tProtein = dailyStats?.targetProtein ?: 0.0,
                    fat = summary?.totalFat ?: 0.0,
                    tFat = dailyStats?.targetFat ?: 0.0,
                    carbs = summary?.totalCarbs ?: 0.0,
                    tCarbs = dailyStats?.targetCarbs ?: 0.0
                )
            }

            mealNames.forEach { mealName ->
                val apiMealType = MealTypeMapper.toApi(mealName)
                val mealSummary = summary?.meals?.get(apiMealType)
                val meals = mealSummary?.portions ?: emptyList()
                val totalCals = mealSummary?.totalKcal ?: 0.0
                val totalProtein = mealSummary?.totalProtein ?: 0.0
                val totalFat = mealSummary?.totalFat ?: 0.0
                val totalCarbs = mealSummary?.totalCarbs ?: 0.0

                item {
                    MealHeader(
                        name = mealName,
                        calories = totalCals,
                        protein = totalProtein,
                        fat = totalFat,
                        carbs = totalCarbs,
                        onAddClick = { onAddMealClick(mealName, dateString) }
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
                            onDeleteClick = { viewModel.deleteConsumedMeal(meal.id.toString(), dateString) }
                        )
                    }
                }
            }

            val workouts = summary?.workouts ?: emptyList()
            val burnedKcal = summary?.burnedKcal ?: 0.0

            item {
                WorkoutHeader(
                    totalBurned = burnedKcal,
                    onAddClick = { showAddWorkoutDialog = true }
                )
            }

            if (workouts.isEmpty()) {
                item {
                    Text(
                        "Brak wpisów",
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                items(workouts, key = { it.id }) { workout ->
                    WorkoutItem(
                        workout = workout,
                        onDeleteClick = { workoutToDelete = workout }
                    )
                }
            }
        }
    }

    if (showAddWorkoutDialog) {
        AddWorkoutDialog(
            onDismiss = { showAddWorkoutDialog = false },
            onConfirm = { name, calories ->
                viewModel.addWorkout(name, calories, dateString)
                showAddWorkoutDialog = false
            }
        )
    }

    workoutToDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text("Usuń trening") },
            text = { Text("Czy na pewno chcesz usunąć „${workout.name}”?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorkout(workout.id, dateString)
                        workoutToDelete = null
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) { Text("Anuluj") }
            }
        )
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
    meal: ConsumedPortionDto,
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
                    Text(
                        meal.productName
                            ?: meal.batchMealName?.let { name ->
                                meal.segmentName?.let { "$name ($it)" } ?: name
                            }
                            ?: meal.segmentName
                            ?: "Nieznany produkt",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${meal.consumedWeightG.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${meal.kcal.toInt()} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    
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
fun MealDetailDialog(meal: ConsumedPortionDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(meal.productName ?: meal.segmentName ?: "Nieznany produkt") },
        text = {
            Column {
                Text("Waga: ${meal.consumedWeightG.toInt()}g", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MacroDetail("Białko", meal.protein, ProteinColor)
                    MacroDetail("Tłuszcze", meal.fat, FatColor)
                    MacroDetail("Węgle", meal.carbs, CarbsColor)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Łącznie: ${meal.kcal.toInt()} kcal",
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
fun WorkoutHeader(
    totalBurned: Double,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Trening", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Zwiększa dzienny limit kalorii",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (totalBurned > 0) {
                    Text(
                        "+${totalBurned.toInt()} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onAddClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Dodaj trening",
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
fun WorkoutItem(
    workout: WorkoutActivityDto,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Limit +${workout.caloriesBurned.toInt()} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj trening") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa aktywności") },
                    placeholder = { Text("np. Bieganie, Siłownia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Spalone kalorie (kcal)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val calories = caloriesText.toDoubleOrNull() ?: return@Button
                    onConfirm(name.trim(), calories)
                },
                enabled = name.isNotBlank() && (caloriesText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
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