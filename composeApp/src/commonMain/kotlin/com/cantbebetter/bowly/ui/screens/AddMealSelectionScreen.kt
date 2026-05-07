package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cantbebetter.bowly.data.MockData
import com.cantbebetter.bowly.models.*
import com.cantbebetter.bowly.ui.components.BarcodeScannerView

@Composable
fun AddMealSelectionScreen(
    mealName: String,
    initialMeal: ConsumedMeal? = null,
    onBack: () -> Unit,
    onConfirm: (ConsumedMeal) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedBatchMeal by remember { mutableStateOf<BatchMeal?>(null) }

    // Inicjalizacja przy edycji
    LaunchedEffect(initialMeal) {
        if (initialMeal != null) {
            if (initialMeal.isFromBatch) {
                // Szukamy patelni, z której pochodzą porcje
                val segmentId = initialMeal.portions.firstOrNull()?.segmentId
                selectedBatchMeal = MockData.batchMeals.find { bm ->
                    bm.segments.any { it.id == segmentId }
                }
            } else {
                // Szukamy produktu
                val productId = initialMeal.portions.firstOrNull()?.productId
                selectedProduct = MockData.products.find { it.id == productId }
            }
        }
    }

    if (selectedProduct != null) {
        ProductAddDetail(
            product = selectedProduct!!,
            initialPortion = initialMeal?.portions?.firstOrNull(),
            onBack = { 
                if (initialMeal != null) onBack() else selectedProduct = null 
            },
            onConfirm = { portion ->
                val meal = ConsumedMeal(
                    id = initialMeal?.id ?: "new_${Clock.uniqueId()}",
                    userId = initialMeal?.userId ?: "1",
                    name = selectedProduct!!.name,
                    mealType = mealName,
                    portions = listOf(portion),
                    timestamp = initialMeal?.timestamp ?: Clock.now()
                )
                onConfirm(meal)
            }
        )
    } else if (selectedBatchMeal != null) {
        BatchMealAddDetail(
            batchMeal = selectedBatchMeal!!,
            initialPortions = initialMeal?.portions ?: emptyList(),
            onBack = { 
                if (initialMeal != null) onBack() else selectedBatchMeal = null 
            },
            onConfirm = { portions ->
                val meal = ConsumedMeal(
                    id = initialMeal?.id ?: "new_${Clock.uniqueId()}",
                    userId = initialMeal?.userId ?: "1",
                    name = selectedBatchMeal!!.name,
                    mealType = mealName,
                    portions = portions,
                    timestamp = initialMeal?.timestamp ?: Clock.now(),
                    isFromBatch = true
                )
                onConfirm(meal)
            }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Produkty", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Patelnie", modifier = Modifier.padding(16.dp))
                }
            }

            if (selectedTab == 0) {
                ProductSearchList(onProductSelected = { selectedProduct = it })
            } else {
                BatchMealList(onBatchSelected = { selectedBatchMeal = it })
            }
        }
    }
}

@Composable
fun ProductSearchList(onProductSelected: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var barcodeToPreFill by remember { mutableStateOf<String?>(null) }

    val filteredProducts = remember(query) {
        if (query.isEmpty()) MockData.products
        else MockData.products.filter { 
            it.name.contains(query, ignoreCase = true) || it.barcode == query
        }
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BarcodeScannerView(
                onBarcodeDetected = { code ->
                    val found = MockData.products.find { it.barcode == code }
                    if (found != null) {
                        onProductSelected(found)
                        showScanner = false
                    } else {
                        barcodeToPreFill = code
                        showAddDialog = true
                        showScanner = false
                    }
                },
                onClose = { showScanner = false }
            )
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            preFilledBarcode = barcodeToPreFill,
            onDismiss = { 
                showAddDialog = false
                barcodeToPreFill = null
            },
            onConfirm = { newProduct ->
                MockData.products.add(newProduct)
                onProductSelected(newProduct)
                showAddDialog = false
            }
        )
    }

    Column {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Szukaj produktu...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showScanner = true }) {
                    Icon(Icons.Default.QrCodeScanner, "Skanuj", tint = MaterialTheme.colorScheme.primary)
                }
            },
            singleLine = true
        )
        LazyColumn {
            items(filteredProducts) { product ->
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = { 
                        Text("${product.calories.toInt()} kcal/100g | B: ${product.protein} T: ${product.fat} W: ${product.carbs}") 
                    },
                    modifier = Modifier.clickable { onProductSelected(product) }
                )
            }
        }
    }
}

@Composable
fun BatchMealList(onBatchSelected: (BatchMeal) -> Unit) {
    val activeBatchMeals by remember { derivedStateOf { MockData.batchMeals.filter { !it.isDepleted } } }
    LazyColumn {
        items(activeBatchMeals) { batch ->
            ListItem(
                headlineContent = { Text(batch.name) },
                supportingContent = { Text("${batch.segments.size} składników") },
                modifier = Modifier.clickable { onBatchSelected(batch) }
            )
        }
    }
}

@Composable
fun ProductAddDetail(
    product: Product,
    initialPortion: ConsumedPortion? = null,
    onBack: () -> Unit,
    onConfirm: (ConsumedPortion) -> Unit
) {
    var amountText by remember { 
        mutableStateOf(initialPortion?.originalValue?.let { if(it == 0.0) "" else it.toString() } ?: "") 
    }
    var unitType by remember { 
        mutableStateOf(initialPortion?.originalUnitType ?: "g") 
    } // "g" lub "unit"

    val currentWeight = when {
        amountText.isEmpty() -> 0.0
        else -> amountText.toDoubleOrNull() ?: 0.0
    }
    
    val finalWeight = if (unitType == "unit") currentWeight * product.unitWeightG else currentWeight
    
    val calculatedCals = (finalWeight / 100.0) * product.calories
    val calculatedProtein = (finalWeight / 100.0) * product.protein
    val calculatedFat = (finalWeight / 100.0) * product.fat
    val calculatedCarbs = (finalWeight / 100.0) * product.carbs

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(product.name, style = MaterialTheme.typography.titleLarge)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Wybierz ilość:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sugerowane wielkości
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val unitName = product.unitName
            QuickAmountButton("0.5 $unitName", modifier = Modifier.weight(1f)) { 
                amountText = "0.5"; unitType = "unit" 
            }
            QuickAmountButton("1 $unitName", modifier = Modifier.weight(1f)) { 
                amountText = "1"; unitType = "unit" 
            }
            QuickAmountButton("2 $unitName", modifier = Modifier.weight(1f)) { 
                amountText = "2"; unitType = "unit" 
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Własny wpis
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.weight(1f),
                label = { Text("Ilość") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            val units = listOf("g", product.unitName)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(if (unitType == "g") "g" else product.unitName)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    units.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u) },
                            onClick = { 
                                unitType = if (u == "g") "g" else "unit"
                                expanded = false 
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Podsumowanie makro
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Podsumowanie porcji", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${calculatedCals.toInt()} kcal")
                Text("B: ${calculatedProtein.toInt()}g | T: ${calculatedFat.toInt()}g | W: ${calculatedCarbs.toInt()}g")
            }
        }
        
        Button(
            onClick = {
                val portion = ConsumedPortion(
                    id = "cp_${Clock.uniqueId()}",
                    segmentName = product.name,
                    consumedWeightG = finalWeight,
                    calories = calculatedCals,
                    protein = calculatedProtein,
                    fat = calculatedFat,
                    carbs = calculatedCarbs,
                    originalUnitType = unitType,
                    originalValue = currentWeight,
                    productId = product.id
                )
                onConfirm(portion)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            enabled = finalWeight > 0
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Potwierdź i dodaj")
        }
    }
}

@Composable
fun BatchMealAddDetail(
    batchMeal: BatchMeal,
    initialPortions: List<ConsumedPortion> = emptyList(),
    onBack: () -> Unit,
    onConfirm: (List<ConsumedPortion>) -> Unit
) {
    var mode by remember { 
        mutableStateOf(initialPortions.firstOrNull()?.originalUnitType ?: "percent") 
    } // "percent" lub "g"
    var commonValueText by remember { 
        mutableStateOf(if (initialPortions.isEmpty()) "25" else "") 
    } // Domyślnie 25% (jedna z 4 porcji)

    val segmentsValues = remember {
        mutableStateMapOf<String, String>().apply {
            batchMeal.segments.forEach { segment ->
                val initial = initialPortions.find { it.segmentId == segment.id }
                this[segment.id] = initial?.originalValue?.let { if(it == 0.0) "" else it.toString() } ?: ""
            }
        }
    }

    // Aktualizacja pól na podstawie wartości wspólnej (tylko w trybie procentowym)
    LaunchedEffect(commonValueText, mode) {
        if (mode == "percent") {
            batchMeal.segments.forEach { segment ->
                segmentsValues[segment.id] = commonValueText
            }
        } else if (commonValueText.isEmpty()) {
            // Jeśli czyścimy common, czyścimy wszystko
            batchMeal.segments.forEach { segment ->
                segmentsValues[segment.id] = ""
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(batchMeal.name, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Jak odmierzasz posiłek?", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            FilterChip(
                selected = mode == "percent",
                onClick = {
                    mode = "percent"
                    commonValueText = "25"
                },
                label = { Text("Procentowo (%)") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = mode == "g",
                onClick = {
                    mode = "g"
                    commonValueText = ""
                },
                label = { Text("W gramach (g)") }
            )
        }

        OutlinedTextField(
            value = commonValueText,
            onValueChange = { commonValueText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (mode == "percent") "Ile procent całej patelni? (%)" else "Ustaw wspólną wagę (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Text(
            "Wartości poszczególnych sekcji:",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(batchMeal.segments) { segment ->
                val segmentValue = segmentsValues[segment.id] ?: ""

                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(segment.name, fontWeight = FontWeight.Bold)
                            val weight = if (mode == "percent") {
                                ((segmentValue.toDoubleOrNull() ?: 0.0) / 100.0) * segment.initialWeightG
                            } else {
                                segmentValue.toDoubleOrNull() ?: 0.0
                            }
                            Text("Waga: ${weight.toInt()}g", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${((weight / 100.0) * segment.product.calories).toInt()} kcal",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedTextField(
                            value = segmentValue,
                            onValueChange = { segmentsValues[segment.id] = it },
                            modifier = Modifier.width(100.dp),
                            label = { Text(if (mode == "percent") "%" else "g") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val portions = batchMeal.segments.mapNotNull { segment ->
                    val value = segmentsValues[segment.id]?.toDoubleOrNull() ?: 0.0
                    if (value <= 0) return@mapNotNull null

                    val weight = if (mode == "percent") (value / 100.0) * segment.initialWeightG else value
                    ConsumedPortion(
                        id = "cp_${Clock.uniqueId()}_${segment.id}",
                        segmentName = segment.name,
                        consumedWeightG = weight,
                        calories = (weight / 100.0) * segment.product.calories,
                        protein = (weight / 100.0) * segment.product.protein,
                        fat = (weight / 100.0) * segment.product.fat,
                        carbs = (weight / 100.0) * segment.product.carbs,
                        originalUnitType = mode,
                        originalValue = value,
                        segmentId = segment.id
                    )
                }
                if (portions.isNotEmpty()) {
                    onConfirm(portions)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            enabled = segmentsValues.values.any { (it.toDoubleOrNull() ?: 0.0) > 0 }
        ) {
            Text("Dodaj wybrane części")
        }
    }
}

@Composable
fun QuickAmountButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(4.dp)) {
        Text(label, fontSize = 12.sp)
    }
}
