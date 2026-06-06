package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.cantbebetter.bowly.ui.components.TareWeightSelector
import com.cantbebetter.bowly.ui.components.resolveNetWeight
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

private fun gramWeightFromInput(
    grossText: String,
    tareEnabled: Boolean,
    containerId: Long?,
    containers: List<WeighingContainerDto>
): Double {
    if (!tareEnabled) return grossText.toDoubleOrNull() ?: 0.0
    return resolveNetWeight(true, grossText, containerId, containers) ?: 0.0
}

@Composable
fun AddMealSelectionScreen(
    viewModel: MainViewModel,
    mealName: String,
    date: String,
    initialMeal: ConsumedPortionDto? = null,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }
    var selectedBatchMeal by remember { mutableStateOf<BatchMealDto?>(null) }
    var selectedRecipe by remember { mutableStateOf<RecipeDto?>(null) }

    val activeBatchMeals by viewModel.activeBatchMeals.collectAsState()
    val scannedProduct by viewModel.scannedProduct.collectAsState()

    LaunchedEffect(scannedProduct) {
        scannedProduct?.let {
            selectedProduct = it
            viewModel.scannedProductHandled()
        }
    }

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
                    name = initialMeal.productName ?: initialMeal.segmentName ?: "Nieznany produkt",
                    calories = (initialMeal.kcal / initialMeal.consumedWeightG) * 100.0,
                    protein = (initialMeal.protein / initialMeal.consumedWeightG) * 100.0,
                    fat = (initialMeal.fat / initialMeal.consumedWeightG) * 100.0,
                    carbohydrates = (initialMeal.carbs / initialMeal.consumedWeightG) * 100.0
                )
            }
        }
    }

    if (selectedProduct != null) {
        ProductAddDetail(
            viewModel = viewModel,
            product = selectedProduct!!,
            mealType = mealName,
            date = date,
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
            initialMealType = mealName,
            initialDate = date,
            isPredefined = true,
            initialPortions = if (initialMeal != null) listOf(initialMeal) else emptyList(),
            onBack = { 
                if (initialMeal != null) onBack() else selectedBatchMeal = null 
            },
            onConfirm = { 
                onConfirm()
            }
        )
    } else if (selectedRecipe != null) {
        BatchMealAddDetail(
            viewModel = viewModel,
            batchMeal = selectedRecipe!!.toVirtualBatchMeal(),
            sourceRecipe = selectedRecipe,
            initialMealType = mealName,
            initialDate = date,
            isPredefined = true,
            onBack = { selectedRecipe = null },
            onConfirm = { onConfirm() }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                }
                Text(
                    text = "Dodaj do: $mealName",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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
                    onProductSelected = { product ->
                        viewModel.cacheProduct(product) { saved ->
                            selectedProduct = saved
                        }
                    }
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
        viewModel.searchRecipes(query, scope = "MINE")
    }

    Column {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Szukaj przepisu...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        if (searchResults.isEmpty()) {
            Text(
                "Brak przepisów. Dodaj je w profilu → Moje produkty i przepisy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn {
            items(searchResults) { recipe ->
                ListItem(
                    headlineContent = { Text(recipe.name) },
                    supportingContent = {
                        Text("${recipe.sections.size} sekcji · ${recipe.sections.sumOf { it.ingredients.size }} składników")
                    },
                    trailingContent = {
                        IconButton(onClick = { onRecipeSelected(recipe) }) {
                            Icon(Icons.Default.Add, contentDescription = "Dodaj przepis do posiłku")
                        }
                    },
                    modifier = Modifier.clickable { onRecipeSelected(recipe) }
                )
            }
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
    
    val barcodeToPrefill by viewModel.barcodeToPrefill.collectAsState()
    val displayProducts by viewModel.displaySearchResults.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLocalProducts()
    }

    LaunchedEffect(query) {
        viewModel.onProductSearchQueryChanged(query)
    }

    LaunchedEffect(barcodeToPrefill) {
        if (barcodeToPrefill != null) {
            showAddDialog = true
        }
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BarcodeScannerView(
                onBarcodeDetected = { code ->
                    viewModel.searchProductByBarcode(code)
                    query = code
                    showScanner = false
                },
                onClose = { showScanner = false }
            )
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            barcodeToPreFill = barcodeToPrefill,
            onDismiss = { 
                showAddDialog = false
                viewModel.barcodeHandled()
            },
            onConfirm = { newProduct ->
                viewModel.saveLocalProduct(newProduct)
                onProductSelected(newProduct)
                showAddDialog = false
                viewModel.barcodeHandled()
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
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        if (error != null) {
            Text(
                text = "Błąd: $error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn {
            items(displayProducts) { product ->
                ListItem(
                    headlineContent = { Text(product.name) },
                    supportingContent = { 
                        Text("${product.calories.toInt()} kcal/100g | B: ${product.protein} T: ${product.fat} W: ${product.carbohydrates}") 
                    },
                    trailingContent = {
                        if (product.source == "LOCAL" || product.source == "USER") {
                            Badge { Text("WŁASNY") }
                        }
                    },
                    modifier = Modifier.clickable {
                        viewModel.cacheProduct(product) { saved ->
                            onProductSelected(saved)
                        }
                    }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nie możesz znaleźć produktu? Dodaj go ręcznie")
                }
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

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(activeBatchMeals) { batch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBatchSelected(batch) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(batch.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${batch.totalCurrentWeightG().toInt()}g · ${batch.totalRemainingKcal().toInt()} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    batch.segments.forEach { segment ->
                        BatchMealSegmentRow(segment)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductAddDetail(
    viewModel: MainViewModel,
    product: ProductDto,
    mealType: String,
    date: String,
    initialPortion: ConsumedPortionDto? = null,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var amountText by remember { 
        mutableStateOf(initialPortion?.consumedWeightG?.let { if(it == 0.0) "" else it.toString() } ?: "") 
    }
    var unitType by remember { 
        mutableStateOf("g")
    }
    var tareEnabled by remember { mutableStateOf(false) }
    var selectedContainerId by remember { mutableStateOf<Long?>(null) }
    val containers by viewModel.containers.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadContainers() }

    val currentAmount = amountText.toDoubleOrNull() ?: 0.0
    
    val hasUnitInfo = product.unitWeightG != null && product.unitWeightG > 0
    val unitWeightG = product.unitWeightG ?: 100.0
    val unitName = product.unitName ?: "szt"
    
    val finalWeight = when {
        unitType == "unit" -> currentAmount * unitWeightG
        tareEnabled -> resolveNetWeight(true, amountText, selectedContainerId, containers) ?: 0.0
        else -> currentAmount
    }
    
    val calculatedCals = (finalWeight / 100.0) * product.calories
    val calculatedProtein = (finalWeight / 100.0) * product.protein
    val calculatedFat = (finalWeight / 100.0) * product.fat
    val calculatedCarbs = (finalWeight / 100.0) * product.carbohydrates

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(product.name, style = MaterialTheme.typography.titleLarge)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Wybierz ilość:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sugerowane wielkości
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasUnitInfo) {
                QuickAmountButton("0.5 $unitName", modifier = Modifier.weight(1f)) {
                    amountText = "0.5"; unitType = "unit"
                }
                QuickAmountButton("1 $unitName", modifier = Modifier.weight(1f)) {
                    amountText = "1"; unitType = "unit"
                }
                QuickAmountButton("2 $unitName", modifier = Modifier.weight(1f)) {
                    amountText = "2"; unitType = "unit"
                }
            } else {
                QuickAmountButton("50g", modifier = Modifier.weight(1f)) {
                    amountText = "50"; unitType = "g"
                }
                QuickAmountButton("100g", modifier = Modifier.weight(1f)) {
                    amountText = "100"; unitType = "g"
                }
                QuickAmountButton("250g", modifier = Modifier.weight(1f)) {
                    amountText = "250"; unitType = "g"
                }
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
            val units = if (hasUnitInfo) listOf("g", unitName) else listOf("g")
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
        
        if (unitType == "g") {
            Spacer(modifier = Modifier.height(16.dp))
            TareWeightSelector(
                containers = containers,
                enabled = tareEnabled,
                onEnabledChange = { tareEnabled = it },
                selectedContainerId = selectedContainerId,
                onContainerSelected = { selectedContainerId = it },
                grossWeightText = amountText,
                onGrossWeightChange = { amountText = it },
                showGrossInput = false
            )
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
                    product = product,
                    weightG = finalWeight,
                    mealDate = date,
                    mealType = MealTypeMapper.toApi(mealType)
                )
                
                if (initialPortion != null) {
                    viewModel.updateConsumedMeal(initialPortion.id.toString(), request, date)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchMealAddDetail(
    viewModel: MainViewModel,
    batchMeal: BatchMealDto,
    sourceRecipe: RecipeDto? = null,
    initialMealType: String,
    initialDate: String,
    isPredefined: Boolean,
    initialPortions: List<ConsumedPortionDto> = emptyList(),
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var mode by remember {
        mutableStateOf("percent")
    }
    var commonValueText by remember(sourceRecipe, initialPortions) {
        mutableStateOf(
            when {
                initialPortions.isNotEmpty() -> ""
                sourceRecipe != null -> "100"
                else -> "25"
            }
        )
    }

    val segmentsValues = remember {
        mutableStateMapOf<Long, String>().apply {
            batchMeal.segments.forEach { segment ->
                val initial = initialPortions.find { it.segmentName == segment.name }
                this[segment.id] = initial?.consumedWeightG?.let { if(it == 0.0) "" else it.toString() } ?: ""
            }
        }
    }
    
    var selectedTimestamp by remember { mutableStateOf(Clock.now()) } // Będziemy używać Date.parse w przyszłości, na razie Clock.now()
    var showDatePicker by remember { mutableStateOf(false) }
    
    val mealTypes = listOf("Śniadanie", "Drugie śniadanie", "Obiad", "Podwieczorek", "Kolacja", "Inne")
    var selectedMealType by remember { mutableStateOf(initialMealType) }
    var expandedMealDropdown by remember { mutableStateOf(false) }
    var tareEnabled by remember { mutableStateOf(false) }
    var selectedContainerId by remember { mutableStateOf<Long?>(null) }
    val containers by viewModel.containers.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadContainers() }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp
    )

    // Propaguje wspólną wartość do wszystkich sekcji (procent lub gramy)
    LaunchedEffect(commonValueText, mode) {
        batchMeal.segments.forEach { segment ->
            segmentsValues[segment.id] = commonValueText
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(batchMeal.name, style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
        Spacer(modifier = Modifier.height(8.dp))

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
            label = { Text(if (mode == "percent") "Ile procent pozostałości? (%)" else "Ustaw wspólną wagę (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        if (mode == "g") {
            TareWeightSelector(
                containers = containers,
                enabled = tareEnabled,
                onEnabledChange = { tareEnabled = it },
                selectedContainerId = selectedContainerId,
                onContainerSelected = { selectedContainerId = it },
                grossWeightText = commonValueText,
                onGrossWeightChange = { commonValueText = it },
                modifier = Modifier.padding(top = 8.dp),
                showGrossInput = false
            )
        }

        Text(
            "Wartości poszczególnych sekcji:",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        batchMeal.segments.forEach { segment ->
                val segmentValue = segmentsValues[segment.id] ?: ""
                val assignedWeight = if (mode == "percent") {
                    ((segmentValue.toDoubleOrNull() ?: 0.0) / 100.0) * segment.currentWeightG
                } else {
                    gramWeightFromInput(segmentValue, tareEnabled, selectedContainerId, containers)
                }
                val maxWeight = segment.currentWeightG
                val remainingAfter = (maxWeight - assignedWeight).coerceAtLeast(0.0)
                val isOverLimit = assignedWeight > maxWeight && assignedWeight > 0

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = if (isOverLimit) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                    } else {
                        CardDefaults.cardColors()
                    }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(segment.name, fontWeight = FontWeight.Bold)
                                segment.product?.let {
                                    Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "Dostępne: ${maxWeight.toInt()}g · ${segment.remainingKcal().toInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            OutlinedTextField(
                                value = segmentValue,
                                onValueChange = { segmentsValues[segment.id] = it },
                                modifier = Modifier.width(100.dp),
                                label = { Text(if (mode == "percent") "%" else "g") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = isOverLimit
                            )
                        }

                        if (assignedWeight > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nałożysz: ${assignedWeight.toInt()}g · ${segment.kcalForWeight(assignedWeight).toInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Zostanie: ${remainingAfter.toInt()}g · ${segment.kcalForWeight(remainingAfter).toInt()} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isOverLimit) {
                            Text(
                                "Maksymalnie ${maxWeight.toInt()}g w tej sekcji",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { segment.remainingProgress() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    }
                }
            }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (isPredefined) {
            Text("Data posiłku:", style = MaterialTheme.typography.labelMedium)
            Text(initialDate, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Przypisz do posiłku:", style = MaterialTheme.typography.labelMedium)
            Text(initialMealType, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
        } else {
            Text("Data posiłku:", style = MaterialTheme.typography.labelMedium)
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(modifier = Modifier.width(8.dp))
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
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
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
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val finalDate = if (isPredefined) initialDate else Clock.formatToApiDate(selectedTimestamp)
                val finalMealType = if (isPredefined) initialMealType else selectedMealType

                if (sourceRecipe != null) {
                    val segmentWeights = batchMeal.segments.mapNotNull { segment ->
                        val value = segmentsValues[segment.id]?.toDoubleOrNull() ?: return@mapNotNull null
                        if (value <= 0) return@mapNotNull null
                        val weight = if (mode == "percent") {
                            (value / 100.0) * segment.currentWeightG
                        } else {
                            gramWeightFromInput(segmentsValues[segment.id] ?: "", tareEnabled, selectedContainerId, containers)
                        }
                        if (weight <= 0 || weight > segment.currentWeightG) return@mapNotNull null
                        segment.id to weight
                    }.toMap()

                    viewModel.consumeRecipePortions(
                        recipe = sourceRecipe,
                        segmentWeights = segmentWeights,
                        mealType = finalMealType,
                        date = finalDate,
                        onComplete = onConfirm
                    )
                    return@Button
                }

                batchMeal.segments.forEach { segment ->
                    val value = segmentsValues[segment.id]?.toDoubleOrNull() ?: 0.0
                    if (value <= 0) return@forEach

                    val weight = if (mode == "percent") {
                        (value / 100.0) * segment.currentWeightG
                    } else {
                        gramWeightFromInput(segmentsValues[segment.id] ?: "", tareEnabled, selectedContainerId, containers)
                    }
                    if (weight <= 0 || weight > segment.currentWeightG) return@forEach
                    
                    viewModel.consumePortion(
                        request = ConsumePortionRequest(
                            segmentId = segment.id,
                            weightG = weight,
                            mealDate = finalDate,
                            mealType = MealTypeMapper.toApi(finalMealType)
                        ),
                        date = finalDate
                    )
                }
                onConfirm()
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            enabled = batchMeal.segments.any { segment ->
                val value = segmentsValues[segment.id]?.toDoubleOrNull() ?: 0.0
                if (value <= 0) return@any false
                val weight = if (mode == "percent") {
                    (value / 100.0) * segment.currentWeightG
                } else {
                    gramWeightFromInput(segmentsValues[segment.id] ?: "", tareEnabled, selectedContainerId, containers)
                }
                weight > 0 && weight <= segment.currentWeightG
            }
        ) {
            Text("Dodaj wybrane części")
        }
        }
    }
    
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

@Composable
fun QuickAmountButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(4.dp)) {
        Text(label, fontSize = 12.sp)
    }
}