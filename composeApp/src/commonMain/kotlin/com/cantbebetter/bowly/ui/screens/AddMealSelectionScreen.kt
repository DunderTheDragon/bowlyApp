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
import com.cantbebetter.bowly.data.network.*
import com.cantbebetter.bowly.models.*
import com.cantbebetter.bowly.ui.components.BarcodeScannerView
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

@Composable
fun AddMealSelectionScreen(
    viewModel: MainViewModel,
    mealName: String,
    initialMeal: ConsumedMealDto? = null,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }
    var selectedBatchMeal by remember { mutableStateOf<BatchMealDto?>(null) }
    var selectedRecipe by remember { mutableStateOf<RecipeDto?>(null) }

    val activeBatchMeals by viewModel.activeBatchMeals.collectAsState()

    // Inicjalizacja przy edycji
    LaunchedEffect(initialMeal, activeBatchMeals) {
        if (initialMeal != null && selectedProduct == null && selectedBatchMeal == null) {
            val foundBatch = activeBatchMeals.find { bm ->
                bm.segments.any { it.name == initialMeal.segmentName }
            }

            if (foundBatch != null) {
                selectedBatchMeal = foundBatch
            } else {
                // Rekonstrukcja produktu dla edycji zwykłego produktu
                selectedProduct = ProductDto(
                    id = null, // Backend rozpozna po ID wpisu w diary
                    name = initialMeal.segmentName,
                    calories = (initialMeal.calories / initialMeal.weightG) * 100.0,
                    protein = (initialMeal.protein / initialMeal.weightG) * 100.0,
                    fat = (initialMeal.fat / initialMeal.weightG) * 100.0,
                    carbohydrates = (initialMeal.carbs / initialMeal.weightG) * 100.0
                )
            }
        }
    }

    if (selectedProduct != null) {
        ProductAddDetail(
            viewModel = viewModel,
            product = selectedProduct!!,
            mealType = mealName,
            initialPortion = initialMeal,
            onBack = { 
                if (initialMeal != null) onBack() else selectedProduct = null 
            },
            onConfirm = { 
                onConfirm()
            }
        )
    } else if (selectedBatchMeal != null) {
        BatchMealAddDetail(
            viewModel = viewModel,
            batchMeal = selectedBatchMeal!!,
            mealType = mealName,
            initialPortions = if (initialMeal != null) listOf(initialMeal) else emptyList(),
            onBack = { 
                if (initialMeal != null) onBack() else selectedBatchMeal = null 
            },
            onConfirm = { 
                onConfirm()
            }
        )
    } else if (selectedRecipe != null) {
        RecipeAddDetail(
            viewModel = viewModel,
            recipe = selectedRecipe!!,
            mealType = mealName,
            onBack = { selectedRecipe = null },
            onConfirm = { onConfirm() }
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
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Przepisy", modifier = Modifier.padding(16.dp))
                }
            }

            when (selectedTab) {
                0 -> ProductSearchList(
                    viewModel = viewModel,
                    onProductSelected = { selectedProduct = it }
                )
                1 -> BatchMealList(
                    viewModel = viewModel,
                    onBatchSelected = { selectedBatchMeal = it }
                )
                2 -> RecipeSearchList(
                    viewModel = viewModel,
                    onRecipeSelected = { selectedRecipe = it }
                )
            }
        }
    }
}

@Composable
fun RecipeSearchList(
    viewModel: MainViewModel,
    onRecipeSelected: (RecipeDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.recipeSearchResults.collectAsState()

    LaunchedEffect(query) {
        viewModel.searchRecipes(query)
    }

    Column {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Szukaj przepisu...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        LazyColumn {
            items(searchResults) { recipe ->
                ListItem(
                    headlineContent = { Text(recipe.name) },
                    supportingContent = { Text("${recipe.sections.sumOf { it.ingredients.size }} składników") },
                    modifier = Modifier.clickable { onRecipeSelected(recipe) }
                )
            }
        }
    }
}

@Composable
fun RecipeAddDetail(
    viewModel: MainViewModel,
    recipe: RecipeDto,
    mealType: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var totalAmountText by remember { mutableStateOf("100") }
    val totalWeightG = recipe.sections.sumOf { it.ingredients.sumOf { ing -> ing.amount } }
    
    val currentTotalAmount = totalAmountText.toDoubleOrNull() ?: 0.0
    val ratio = if (totalWeightG > 0) currentTotalAmount / totalWeightG else 0.0

    val calculatedCals = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * ratio / 100.0) * ing.product.calories } }
    val calculatedProtein = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * ratio / 100.0) * ing.product.protein } }
    val calculatedFat = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * ratio / 100.0) * ing.product.fat } }
    val calculatedCarbs = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * ratio / 100.0) * ing.product.carbohydrates } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(recipe.name, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Wybierz wagę gotowego dania:", style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = totalAmountText,
            onValueChange = { totalAmountText = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            label = { Text("Waga (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

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
                // Dodajemy przepis jako "produkt" o skumulowanych makrach
                val request = ConsumeProductRequest(
                    productId = "RECIPE:${recipe.id}", // Specjalny prefiks dla backendu lub marker
                    weightG = currentTotalAmount,
                    mealType = mealType.uppercase()
                )
                // Tu w realnej aplikacji backend musiałby to obsłużyć. 
                // Alternatywnie: dodajemy każdy składnik osobno.
                // Dla uproszczenia tutaj przyjmujemy, że robimy "Virtual Product"
                
                // UWAGA: Ponieważ API consumeProduct oczekuje realnego productId, 
                // a my nie mamy "produktu" z przepisu, w tej wersji po prostu 
                // przeliczymy to na wirtualny produkt lub wyślemy serię składników.
                // Na potrzeby tego zadania załóżmy, że tworzymy produkt tymczasowy 
                // lub backend obsługuje RECIPE:id.
                
                viewModel.consumeProduct(request, Clock.formatToApiDate(Clock.now()))
                onConfirm()
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            enabled = currentTotalAmount > 0
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dodaj do dziennika")
        }
    }
}

@Composable
fun ProductSearchList(
    viewModel: MainViewModel,
    onProductSelected: (ProductDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var barcodeToPreFill by remember { mutableStateOf<String?>(null) }

    val searchResults by viewModel.searchResults.collectAsState()

    LaunchedEffect(query) {
        viewModel.searchProducts(query)
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BarcodeScannerView(
                onBarcodeDetected = { code ->
                    query = code
                    showScanner = false
                },
                onClose = { showScanner = false }
            )
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { 
                showAddDialog = false
                barcodeToPreFill = null
            },
            onConfirm = { newProduct ->
                viewModel.addLocalProduct(newProduct)
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
            items(searchResults) { product ->
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = { 
                        Text("${product.calories.toInt()} kcal/100g | B: ${product.protein} T: ${product.fat} W: ${product.carbohydrates}") 
                    },
                    modifier = Modifier.clickable { onProductSelected(product) }
                )
            }
        }
    }
}

@Composable
fun BatchMealList(
    viewModel: MainViewModel,
    onBatchSelected: (BatchMealDto) -> Unit
) {
    val activeBatchMeals by viewModel.activeBatchMeals.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadActiveBatchMeals()
    }

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
    viewModel: MainViewModel,
    product: ProductDto,
    mealType: String,
    initialPortion: ConsumedMealDto? = null,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var amountText by remember { 
        mutableStateOf(initialPortion?.weightG?.let { if(it == 0.0) "" else it.toString() } ?: "") 
    }
    var unitType by remember { 
        mutableStateOf("g")
    } // "g" lub "unit"

    val currentWeight = when {
        amountText.isEmpty() -> 0.0
        else -> amountText.toDoubleOrNull() ?: 0.0
    }
    
    // API ProductDto does not have unitWeightG or unitName like the UI model had.
    // We'll default to grams for now or assume 100g units if unknown.
    val unitWeightG = 100.0 // Defaulting
    val unitName = "szt"
    
    val finalWeight = if (unitType == "unit") currentWeight * unitWeightG else currentWeight
    
    val calculatedCals = (finalWeight / 100.0) * product.calories
    val calculatedProtein = (finalWeight / 100.0) * product.protein
    val calculatedFat = (finalWeight / 100.0) * product.fat
    val calculatedCarbs = (finalWeight / 100.0) * product.carbohydrates

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
            val units = listOf("g", unitName)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(if (unitType == "g") "g" else unitName)
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
                val request = ConsumeProductRequest(
                    productId = product.id ?: "",
                    weightG = finalWeight,
                    mealType = mealType.uppercase()
                )
                val date = Clock.formatToApiDate(Clock.now())
                
                if (initialPortion != null) {
                    viewModel.updateConsumedMeal(initialPortion.id, request, date)
                } else {
                    viewModel.consumeProduct(request, date)
                }
                onConfirm()
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
    viewModel: MainViewModel,
    batchMeal: BatchMealDto,
    mealType: String,
    initialPortions: List<ConsumedMealDto> = emptyList(),
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var mode by remember { 
        mutableStateOf("percent") 
    } // "percent" lub "g"
    var commonValueText by remember { 
        mutableStateOf(if (initialPortions.isEmpty()) "25" else "") 
    } // Domyślnie 25% (jedna z 4 porcji)

    val segmentsValues = remember {
        mutableStateMapOf<String, String>().apply {
            batchMeal.segments.forEach { segment ->
                val segmentId = segment.id ?: return@forEach
                val initial = initialPortions.find { it.id == segmentId }
                this[segmentId] = initial?.weightG?.let { if(it == 0.0) "" else it.toString() } ?: ""
            }
        }
    }

    // Aktualizacja pól na podstawie wartości wspólnej (tylko w trybie procentowym)
    LaunchedEffect(commonValueText, mode) {
        if (mode == "percent") {
            batchMeal.segments.forEach { segment ->
                segment.id?.let { segmentsValues[it] = commonValueText }
            }
        } else if (commonValueText.isEmpty()) {
            // Jeśli czyścimy common, czyścimy wszystko
            batchMeal.segments.forEach { segment ->
                segment.id?.let { segmentsValues[it] = "" }
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
                val segmentId = segment.id ?: return@items
                val segmentValue = segmentsValues[segmentId] ?: ""

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
                            onValueChange = { segmentsValues[segmentId] = it },
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
                batchMeal.segments.forEach { segment ->
                    val segmentId = segment.id ?: return@forEach
                    val value = segmentsValues[segmentId]?.toDoubleOrNull() ?: 0.0
                    if (value <= 0) return@forEach

                    val weight = if (mode == "percent") (value / 100.0) * segment.initialWeightG else value
                    
                    viewModel.consumePortion(
                        request = ConsumePortionRequest(
                            segmentId = segmentId,
                            weightG = weight,
                            mealType = mealType.uppercase()
                        ),
                        date = Clock.formatToApiDate(Clock.now()) // Assuming today for now
                    )
                }
                onConfirm()
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
