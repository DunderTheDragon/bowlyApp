package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.data.MockData
import com.cantbebetter.bowly.models.*
import com.cantbebetter.bowly.ui.screens.Clock

@Composable
fun BatchMealsScreen() {
    var selectedMeal by remember { mutableStateOf<BatchMeal?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

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
                Text("Aktywne Patelnie", style = MaterialTheme.typography.headlineMedium)
            }
            items(MockData.batchMeals.filter { !it.isDepleted }) { meal ->
                MealCard(meal, onTakePortion = { selectedMeal = meal })
            }
        }
    }

    if (selectedMeal != null) {
        TakePortionDialog(
            meal = selectedMeal!!,
            onDismiss = { selectedMeal = null },
            onConfirm = { segment, weight ->
                handleTakePortion(selectedMeal!!.name, segment, weight)
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
fun CreateBatchMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (BatchMeal) -> Unit
) {
    var mealName by remember { mutableStateOf("") }
    // Mapa: ID sekcji -> Listę składników (ProductData)
    val sections = remember { mutableStateListOf<SectionData>(SectionData(id = "s_${Clock.uniqueId()}", name = "")) }
    var activeSectionIdForSearch by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Nowa Patelnia")
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Nazwa dania (np. Gulasz)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sekcje / Podział:", style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = {
                            sections.add(SectionData(id = "s_${Clock.uniqueId()}", name = ""))
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Text("Dodaj sekcję", style = MaterialTheme.typography.labelMedium)
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    items(sections, key = { it.id }) { section ->
                        SectionItem(
                            section = section,
                            isOnlySection = sections.size == 1,
                            onNameChange = { newName ->
                                val index = sections.indexOf(section)
                                sections[index] = section.copy(name = newName)
                            },
                            onRemoveSection = { sections.remove(section) },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSegments = sections.map { section ->
                        // Agregujemy makro dla sekcji
                        val totalCals = section.products.sumOf { (it.weightG / 100.0) * it.product.calories }
                        val totalProt = section.products.sumOf { (it.weightG / 100.0) * it.product.protein }
                        val totalFat = section.products.sumOf { (it.weightG / 100.0) * it.product.fat }
                        val totalCarb = section.products.sumOf { (it.weightG / 100.0) * it.product.carbs }
                        val totalWeight = section.products.sumOf { it.weightG }

                        // Tworzymy wirtualny produkt reprezentujący sekcję
                        val sectionProduct = Product(
                            id = "sp_${section.id}",
                            name = section.name.ifBlank { mealName },
                            calories = if (totalWeight > 0) (totalCals / totalWeight) * 100.0 else 0.0,
                            protein = if (totalWeight > 0) (totalProt / totalWeight) * 100.0 else 0.0,
                            fat = if (totalWeight > 0) (totalFat / totalWeight) * 100.0 else 0.0,
                            carbs = if (totalWeight > 0) (totalCarb / totalWeight) * 100.0 else 0.0,
                            source = "USER"
                        )

                        BatchMealSegment(
                            id = section.id,
                            name = section.name.ifBlank { mealName },
                            product = sectionProduct,
                            initialWeightG = totalWeight,
                            currentWeightG = totalWeight
                        )
                    }
                    val newMeal = BatchMeal(
                        id = "bm_${Clock.uniqueId()}",
                        name = mealName,
                        segments = finalSegments
                    )
                    onConfirm(newMeal)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = mealName.isNotBlank() && sections.any { it.products.isNotEmpty() }
            ) {
                Text("Utwórz patelnię")
            }
        }
    )

    if (activeSectionIdForSearch != null) {
        val currentActiveId = activeSectionIdForSearch // Zapamiętujemy ID w stałej
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
fun SectionItem(
    section: SectionData,
    isOnlySection: Boolean,
    onNameChange: (String) -> Unit,
    onRemoveSection: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductWeightChange: (ProductData, Double) -> Unit,
    onRemoveProduct: (ProductData) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!isOnlySection) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = section.name,
                        onValueChange = onNameChange,
                        label = { Text("Nazwa sekcji (np. Sos, Ryż)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = onRemoveSection) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            section.products.forEach { productData ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(productData.product.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedTextField(
                        value = if (productData.weightG == 0.0) "" else productData.weightG.toInt().toString(),
                        onValueChange = { onProductWeightChange(productData, it.toDoubleOrNull() ?: 0.0) },
                        modifier = Modifier.width(70.dp),
                        label = { Text("g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = { onRemoveProduct(productData) }) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            TextButton(
                onClick = onAddProductClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dodaj produkt do sekcji")
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
                    leadingIcon = { Icon(Icons.Default.Search, null) }
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

@Composable
fun MealCard(meal: BatchMeal, onTakePortion: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(meal.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            meal.segments.forEach { segment ->
                val progress = (segment.currentWeightG / segment.initialWeightG).toFloat()
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(segment.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${segment.currentWeightG.toInt()} / ${segment.initialWeightG.toInt()} g", style = MaterialTheme.typography.bodySmall)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onTakePortion, modifier = Modifier.align(Alignment.End)) {
                Text("Nałóż porcję")
            }
        }
    }
}

@Composable
fun TakePortionDialog(
    meal: BatchMeal,
    onDismiss: () -> Unit,
    onConfirm: (BatchMealSegment, Double) -> Unit
) {
    var selectedSegment by remember { mutableStateOf(meal.segments.first()) }
    var amountText by remember { mutableStateOf("") }
    var isPercentage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ile nakładasz?") },
        text = {
            Column {
                Text("Wybierz segment:")
                meal.segments.forEach { segment ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (selectedSegment == segment),
                            onClick = { selectedSegment = segment }
                        )
                        Text(segment.name)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (isPercentage) "Procent (np. 25)" else "Waga (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPercentage, onCheckedChange = { isPercentage = it })
                    Text("Podaj w % zamiast gramów")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amountText.toDoubleOrNull() ?: 0.0
                val weight = if (isPercentage) (value / 100.0) * selectedSegment.currentWeightG else value
                onConfirm(selectedSegment, weight)
            }) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}

fun handleTakePortion(mealName: String, segment: BatchMealSegment, weight: Double) {
    // Basic logic for MVP - updates MockData
    val consumedWeight = weight.coerceAtMost(segment.currentWeightG)
    
    // Find and update the segment in MockData
    MockData.batchMeals.forEachIndexed { mealIndex, meal ->
        if (meal.segments.any { it.id == segment.id }) {
            val updatedSegments = meal.segments.map { s ->
                if (s.id == segment.id) {
                    s.copy(currentWeightG = s.currentWeightG - consumedWeight)
                } else s
            }
            
            // Sprawdzamy czy cała patelnia jest pusta (suma wag wszystkich segmentów < 0.1g)
            val isDepleted = updatedSegments.sumOf { it.currentWeightG } < 0.1
            
            if (isDepleted) {
                MockData.batchMeals[mealIndex] = meal.copy(segments = updatedSegments, isDepleted = true)
            } else {
                MockData.batchMeals[mealIndex] = meal.copy(segments = updatedSegments)
            }
        }
    }

    // Add to daily log
    val ratio = consumedWeight / 100.0
    val portion = ConsumedPortion(
        id = "p_${Clock.uniqueId()}",
        segmentId = segment.id,
        segmentName = segment.name,
        productId = segment.product.id,
        consumedWeightG = consumedWeight,
        calories = segment.product.calories * ratio,
        protein = segment.product.protein * ratio,
        fat = segment.product.fat * ratio,
        carbs = segment.product.carbs * ratio,
    )

    val consumedMeal = com.cantbebetter.bowly.models.ConsumedMeal(
        id = "m_${Clock.uniqueId()}",
        userId = MockData.currentUser.id,
        name = mealName,
        mealType = "Przekąska",
        portions = listOf(portion),
        timestamp = Clock.now(),
        isFromBatch = true
    )
    MockData.consumedMeals.add(0, consumedMeal)
}
