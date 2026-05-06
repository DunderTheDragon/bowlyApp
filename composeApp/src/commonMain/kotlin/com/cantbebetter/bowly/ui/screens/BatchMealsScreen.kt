package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cantbebetter.bowly.data.MockData
import com.cantbebetter.bowly.models.*

data class SectionData(
    val id: String,
    val name: String,
    val products: List<ProductData> = emptyList()
)

data class ProductData(
    val product: Product,
    val weightG: Double
)

@Composable
fun BatchMealsScreen() {
    var selectedMeal by remember { mutableStateOf<BatchMeal?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(MockData.currentUser.showBatchOnboarding) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj patelnię")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktywne Patelnie", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { showOnboarding = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Instrukcja", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            val activeMeals = MockData.batchMeals.filter { !it.isDepleted }
            if (activeMeals.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Brak aktywnych patelni.\nDodaj nową, aby śledzić posiłki zbiorcze.", 
                            textAlign = TextAlign.Center, 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(activeMeals) { meal ->
                    MealCard(meal, onTakePortion = { selectedMeal = meal })
                }
            }
        }
    }

    if (showOnboarding) {
        BatchOnboardingDialog(
            onDismiss = { 
                showOnboarding = false 
            },
            onDontShowAgain = {
                MockData.updateCurrentUser(MockData.currentUser.copy(showBatchOnboarding = false))
                showOnboarding = false
            }
        )
    }

    if (selectedMeal != null) {
        TakePortionDialog(
            meal = selectedMeal!!,
            onDismiss = { selectedMeal = null },
            onConfirm = { portions, mealType, timestamp ->
                handleTakePortions(selectedMeal!!.name, portions, mealType, timestamp)
                selectedMeal = null
            }
        )
    }

    if (showCreateDialog) {
        CreateBatchMealDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { newMeal ->
                MockData.batchMeals.add(newMeal)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun BatchOnboardingDialog(onDismiss: () -> Unit, onDontShowAgain: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    OnboardingPage(page)
                }
                
                Row(
                    Modifier.height(32.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Rozumiem") }
        },
        dismissButton = {
            TextButton(onClick = onDontShowAgain) { Text("Nie pokazuj więcej") }
        }
    )
}

@Composable
fun OnboardingPage(page: Int) {
    val content = when(page) {
        0 -> Triple(Icons.Default.Restaurant, "Wirtualna Patelnia", "Przygotowujesz posiłek na kilka dni? Zrób to raz, a potem nakładaj porcje bez ponownego liczenia składników.")
        1 -> Triple(Icons.Default.Layers, "Sekcje i Składniki", "Podziel danie na sekcje (np. sos, ryż). Bowly zapamięta proporcje i automatycznie obliczy wartości dla nałożonej wagi.")
        else -> Triple(Icons.Default.QueryStats, "Śledzenie ubytku", "Każda nałożona porcja pomniejsza wagę 'patelni'. Gdy zjesz wszystko, patelnia sama zniknie z listy.")
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(content.first, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(content.second, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(content.third, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun CreateBatchMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (BatchMeal) -> Unit
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var mealName by remember { mutableStateOf("") }
    val sections = remember { mutableStateListOf<SectionData>(SectionData(id = "s_${Clock.uniqueId()}", name = "Główna część")) }
    var activeSectionIdForSearch by remember { mutableStateOf<String?>(null) }

    val totalWeight = sections.sumOf { it.products.sumOf { p -> p.weightG } }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                Text("Nowa patelnia", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Meal Name with Header-on-border style
                Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(top = 32.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Podział na części:", style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = {
                                    sections.add(SectionData(id = "s_${Clock.uniqueId()}", name = "Nowa sekcja"))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dodaj sekcję", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                            contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)
                        ) {
                            items(sections, key = { it.id }) { section ->
                                var showDeleteConfirmation by remember { mutableStateOf(false) }

                                if (showDeleteConfirmation) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirmation = false },
                                        title = { Text("Usuń sekcję") },
                                        text = { Text("Czy na pewno chcesz usunąć sekcję \"${section.name}\"?") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    sections.remove(section)
                                                    showDeleteConfirmation = false
                                                }
                                            ) {
                                                Text("Usuń", color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteConfirmation = false }) {
                                                Text("Anuluj")
                                            }
                                        }
                                    )
                                }

                                SectionItem(
                                    section = section,
                                    isOnlySection = sections.size == 1,
                                    onNameChange = { newName ->
                                        val index = sections.indexOf(section)
                                        sections[index] = section.copy(name = newName)
                                    },
                                    onRemoveSection = { showDeleteConfirmation = true },
                                    onAddProductClick = { activeSectionIdForSearch = section.id },
                                    onProductWeightChange = { productData, newWeight ->
                                        val sectionIndex = sections.indexOf(section)
                                        val productIndex = section.products.indexOf(productData)
                                        val updatedProducts = section.products.toMutableList()
                                        updatedProducts[productIndex] = productData.copy(weightG = newWeight)
                                        sections[sectionIndex] = section.copy(products = updatedProducts)
                                    },
                                    onRemoveProduct = { productData ->
                                        val sectionIndex = sections.indexOf(section)
                                        val updatedProducts = section.products.toMutableList()
                                        updatedProducts.remove(productData)
                                        sections[sectionIndex] = section.copy(products = updatedProducts)
                                    }
                                )
                            }
                        }
                    }

                    // Meal Name Header
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-18).dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = mealName,
                            onValueChange = { mealName = it },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.widthIn(min = 140.dp, max = 280.dp),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (mealName.isEmpty()) {
                                    Text(
                                        "Nazwa potrawy",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Edit,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    val finalSegments = sections.map { section ->
                        val totalCals = section.products.sumOf { (it.weightG / 100.0) * it.product.calories }
                        val totalProt = section.products.sumOf { (it.weightG / 100.0) * it.product.protein }
                        val totalFat = section.products.sumOf { (it.weightG / 100.0) * it.product.fat }
                        val totalCarb = section.products.sumOf { (it.weightG / 100.0) * it.product.carbs }
                        val totalWeightG = section.products.sumOf { it.weightG }

                        val sectionProduct = Product(
                            id = "sp_${section.id}",
                            name = section.name.ifBlank { mealName },
                            calories = if (totalWeightG > 0) (totalCals / totalWeightG) * 100.0 else 0.0,
                            protein = if (totalWeightG > 0) (totalProt / totalWeightG) * 100.0 else 0.0,
                            fat = if (totalWeightG > 0) (totalFat / totalWeightG) * 100.0 else 0.0,
                            carbs = if (totalWeightG > 0) (totalCarb / totalWeightG) * 100.0 else 0.0,
                            source = "USER"
                        )

                        BatchMealSegment(
                            id = section.id,
                            name = section.name.ifBlank { mealName },
                            product = sectionProduct,
                            initialWeightG = totalWeightG,
                            currentWeightG = totalWeightG
                        )
                    }
                    val newMeal = BatchMeal(
                        id = "bm_${Clock.uniqueId()}",
                        name = mealName,
                        segments = finalSegments
                    )
                    onConfirm(newMeal)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = mealName.isNotBlank() && sections.any { it.products.isNotEmpty() }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Utwórz patelnię")
                    Text("Łącznie: ${totalWeight.toInt()}g", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    )

    if (activeSectionIdForSearch != null) {
        val currentActiveId = activeSectionIdForSearch 
        SimpleProductSearchDialog(
            onDismiss = { activeSectionIdForSearch = null },
            onProductSelected = { product ->
                val sectionIndex = sections.indexOfFirst { it.id == currentActiveId }
                if (sectionIndex != -1) {
                    val section = sections[sectionIndex]
                    val updatedProducts = section.products.toMutableList()
                    updatedProducts.add(ProductData(product = product, weightG = 0.0))
                    sections[sectionIndex] = section.copy(products = updatedProducts)
                }
                activeSectionIdForSearch = null
            }
        )
    }
}

@Composable
fun SectionItem(
    section: SectionData,
    isOnlySection: Boolean,
    onNameChange: (String) -> Unit,
    onRemoveSection: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductWeightChange: (ProductData, Double) -> Unit,
    onRemoveProduct: (ProductData) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(12.dp)
                )
                .padding(top = 20.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            if (section.products.isEmpty()) {
                Text(
                    "Brak składników w tej sekcji",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                section.products.forEach { productData ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            productData.product.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                        OutlinedTextField(
                            value = if (productData.weightG <= 0.0) "" else productData.weightG.toString().removeSuffix(".0"),
                            onValueChange = { newValue ->
                                val v = newValue.toDoubleOrNull() ?: 0.0
                                onProductWeightChange(productData, v.coerceAtLeast(0.0))
                            },
                            modifier = Modifier.width(90.dp),
                            placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        Text("g", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 6.dp))
                        IconButton(
                            onClick = { onRemoveProduct(productData) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            Button(
                onClick = onAddProductClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Dodaj składnik")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            }
        }

        // Section Name as Header on the Border
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = section.name,
                onValueChange = onNameChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.widthIn(min = 80.dp, max = 160.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edytuj nazwę",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
            if (!isOnlySection) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                IconButton(
                    onClick = onRemoveSection,
                    modifier = Modifier.size(32.dp).padding(start = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakePortionDialog(
    meal: BatchMeal,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Double>, String, Long) -> Unit
) {
    val portions = remember { mutableStateMapOf<String, String>().apply {
        meal.segments.forEach { put(it.id, "") }
    }}
    
    var selectedTimestamp by remember { mutableStateOf(Clock.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val mealTypes = MockData.getMealTypesForDate(selectedTimestamp)
    var selectedMealType by remember { mutableStateOf(if (mealTypes.isNotEmpty()) mealTypes[0] else "Inne") }
    var expandedMealDropdown by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Nałóż porcję") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Wprowadź gramatury dla sekcji:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                meal.segments.forEach { segment ->
                    val weightStr = portions[segment.id] ?: ""
                    val weight = weightStr.toDoubleOrNull() ?: 0.0
                    val isError = weightStr.isNotBlank() && (weight < 0 || weight > segment.currentWeightG)
                    
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(segment.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("pozostało: ${segment.currentWeightG.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { portions[segment.id] = it },
                            modifier = Modifier.fillMaxWidth(),
                            isError = isError,
                            placeholder = { Text("0") },
                            suffix = { Text("g") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = if (isError) {
                                { Text(if (weight < 0) "Wartość nie może być ujemna" else "Za duża waga (max ${segment.currentWeightG.toInt()}g)", color = MaterialTheme.colorScheme.error) }
                            } else null
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Data posiłku:", style = MaterialTheme.typography.labelMedium)
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val date = Instant(selectedTimestamp) // Simplified for mock
                        Text("Dzień: ${if (selectedTimestamp / 86400000L == Clock.now() / 86400000L) "Dzisiaj" else "Inny"}")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Przypisz do posiłku:", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = expandedMealDropdown,
                    onExpandedChange = { expandedMealDropdown = !expandedMealDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedMealType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMealDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMealDropdown,
                        onDismissRequest = { expandedMealDropdown = false }
                    ) {
                        mealTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedMealType = type
                                    expandedMealDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasError = meal.segments.any { 
                val w = portions[it.id]?.toDoubleOrNull() ?: 0.0
                w < 0 || w > it.currentWeightG
            }
            val hasAnyValue = meal.segments.any { (portions[it.id]?.toDoubleOrNull() ?: 0.0) > 0 }

            Button(
                onClick = {
                    val finalPortions = meal.segments.associate { it.id to (portions[it.id]?.toDoubleOrNull() ?: 0.0) }
                    onConfirm(finalPortions, selectedMealType, selectedTimestamp)
                },
                enabled = !hasError && hasAnyValue
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedTimestamp = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

fun handleTakePortions(mealName: String, portionMap: Map<String, Double>, mealType: String, timestamp: Long) {
    val batchMeal = MockData.batchMeals.find { bm -> bm.segments.any { it.id == portionMap.keys.first() } } ?: return
    
    val consumedPortions = mutableListOf<ConsumedPortion>()
    
    portionMap.forEach { (segmentId, weight) ->
        if (weight <= 0) return@forEach
        val segment = batchMeal.segments.find { it.id == segmentId } ?: return@forEach
        val consumedWeight = weight.coerceAtMost(segment.currentWeightG)
        
        val ratio = consumedWeight / 100.0
        consumedPortions.add(ConsumedPortion(
            id = "p_${Clock.uniqueId()}",
            segmentId = segment.id,
            segmentName = segment.name,
            productId = segment.product.id,
            consumedWeightG = consumedWeight,
            calories = segment.product.calories * ratio,
            protein = segment.product.protein * ratio,
            fat = segment.product.fat * ratio,
            carbs = segment.product.carbs * ratio,
        ))
    }

    if (consumedPortions.isEmpty()) return

    val consumedMeal = com.cantbebetter.bowly.models.ConsumedMeal(
        id = "m_${Clock.uniqueId()}",
        userId = MockData.currentUser.id,
        name = mealName,
        mealType = mealType,
        portions = consumedPortions,
        timestamp = timestamp,
        isFromBatch = true
    )
    MockData.upsertConsumedMeal(consumedMeal)
}

@Composable
fun MealCard(meal: BatchMeal, onTakePortion: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(meal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            meal.segments.forEach { segment ->
                val progress = (segment.currentWeightG / segment.initialWeightG).toFloat()
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(segment.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${segment.currentWeightG.toInt()} / ${segment.initialWeightG.toInt()} g", style = MaterialTheme.typography.bodySmall)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onTakePortion, 
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.RestaurantMenu, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nałóż porcję do posiłku")
            }
        }
    }
}

@Composable
fun SimpleProductSearchDialog(onDismiss: () -> Unit, onProductSelected: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filteredProducts = remember(query) {
        if (query.isEmpty()) MockData.products
        else MockData.products.filter { it.name.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wyszukaj składnik") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Np. Kurczak...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filteredProducts) { product ->
                        ListItem(
                            headlineContent = { Text(product.name) },
                            modifier = Modifier.clickable { onProductSelected(product) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}
