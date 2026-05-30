package com.cantbebetter.bowly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cantbebetter.bowly.data.network.ProductDto
import com.cantbebetter.bowly.data.network.RecipeDto
import com.cantbebetter.bowly.data.network.RecipeIngredientDto
import com.cantbebetter.bowly.data.network.RecipeSectionDto
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Produkty", "Przepisy")

    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<ProductDto?>(null) }
    var editingRecipe by remember { mutableStateOf<RecipeDto?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    
    // Placeholder for search results, should ideally come from viewModel
    val localProducts by viewModel.localProducts.collectAsState()
    val recipeSearchResults by viewModel.recipeSearchResults.collectAsState()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            viewModel.loadLocalProducts()
        } else {
            viewModel.loadRecipes(scope = "MINE")
        }
    }

    LaunchedEffect(selectedTab, searchQuery) {
        if (selectedTab == 1) {
            kotlinx.coroutines.delay(300)
            viewModel.searchRecipes(searchQuery, scope = "MINE")
        }
    }

    val filteredProducts = remember(localProducts, searchQuery) {
        if (searchQuery.isEmpty()) {
            localProducts
        } else {
            localProducts.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Moje produkty i przepisy") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (selectedTab == 0) {
                    showAddDialog = true
                } else {
                    showAddRecipeDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(if (selectedTab == 0) "Szukaj w produktach..." else "Szukaj w przepisach...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            if (selectedTab == 0) {
                // Products Tab
                if (filteredProducts.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Brak wyników", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredProducts) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                supportingContent = {
                                    Text("${product.calories.toInt()} kcal/100g | B: ${product.protein} T: ${product.fat} W: ${product.carbohydrates}")
                                },
                                trailingContent = {
                                    IconButton(onClick = { editingProduct = product }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edytuj")
                                    }
                                },
                                modifier = Modifier.clickable { editingProduct = product }
                            )
                        }
                    }
                }
            } else {
                // Recipes Tab
                if (recipeSearchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (searchQuery.isNotEmpty()) "Brak wyników" else "Brak przepisów — dodaj pierwszy",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(recipeSearchResults) { recipe ->
                            ListItem(
                                headlineContent = { Text(recipe.name) },
                                supportingContent = {
                                    val totalIngredients = recipe.sections.sumOf { it.ingredients.size }
                                    Text("$totalIngredients składników · ${recipe.sections.size} sekcji")
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { editingRecipe = recipe }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edytuj")
                                        }
                                        IconButton(onClick = {
                                            viewModel.deleteRecipe(recipe.id ?: "") {
                                                viewModel.loadRecipes(scope = "MINE")
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Usuń")
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { editingRecipe = recipe }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        EditProductDialog(
            product = ProductDto(
                name = "",
                calories = 0.0,
                protein = 0.0,
                fat = 0.0,
                carbohydrates = 0.0,
                source = "USER"
            ),
            isNew = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { newProduct ->
                viewModel.saveLocalProduct(newProduct)
                showAddDialog = false
            }
        )
    }

    if (editingProduct != null) {
        EditProductDialog(
            product = editingProduct!!,
            isNew = false,
            onDismiss = { editingProduct = null },
            onConfirm = { updatedProduct ->
                viewModel.saveLocalProduct(updatedProduct)
                editingProduct = null
            }
        )
    }

    if (showAddRecipeDialog) {
        RecipeEditor(
            recipe = RecipeDto(name = "", sections = listOf(RecipeSectionDto(name = "Składniki", ingredients = emptyList()))),
            viewModel = viewModel,
            onDismiss = { showAddRecipeDialog = false },
            onConfirm = { newRecipe ->
                viewModel.saveRecipe(newRecipe) {
                    viewModel.loadRecipes(scope = "MINE")
                }
                showAddRecipeDialog = false
            }
        )
    }
    
    if (editingRecipe != null) {
        RecipeEditor(
            recipe = editingRecipe!!,
            viewModel = viewModel,
            onDismiss = { editingRecipe = null },
            onConfirm = { updatedRecipe ->
                viewModel.saveRecipe(updatedRecipe) {
                    viewModel.loadRecipes(scope = "MINE")
                }
                editingRecipe = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditor(
    recipe: RecipeDto,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (RecipeDto) -> Unit
) {
    var name by remember { mutableStateOf(recipe.name) }
    val sections = remember { mutableStateListOf<RecipeSectionDto>().apply { addAll(recipe.sections) } }
    
    var showProductSearch by remember { mutableStateOf<Int?>(null) } // Index sekcji, do której dodajemy
    val localProducts by viewModel.localProducts.collectAsState()
    var searchProductQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(localProducts, searchProductQuery) {
        if (searchProductQuery.isEmpty()) {
            localProducts
        } else {
            localProducts.filter { it.name.contains(searchProductQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (recipe.id == null) "Nowy przepis" else "Edytuj przepis") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Zamknij") }
                    },
                    actions = {
                        Button(
                            onClick = { onConfirm(recipe.copy(name = name, sections = sections.toList())) },
                            enabled = name.isNotBlank() && sections.any { it.ingredients.isNotEmpty() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Zapisz")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { sections.add(RecipeSectionDto(name = "Nowa sekcja", ingredients = emptyList())) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Dodaj sekcję") }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa przepisu") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )

                sections.forEachIndexed { sIndex, section ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = section.name,
                                    onValueChange = { sections[sIndex] = section.copy(name = it) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Nazwa sekcji") },
                                    textStyle = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { sections.removeAt(sIndex) }) {
                                    Icon(Icons.Default.Delete, "Usuń sekcję", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            section.ingredients.forEachIndexed { iIndex, ingredient ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ingredient.product.name, style = MaterialTheme.typography.bodyMedium)
                                        Text("${ingredient.product.calories.toInt()} kcal/100g", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedTextField(
                                        value = if (ingredient.amount == 0.0) "" else ingredient.amount.toString(),
                                        onValueChange = { val newAmount = it.toDoubleOrNull() ?: 0.0
                                            val newIngredients = section.ingredients.toMutableList()
                                            newIngredients[iIndex] = ingredient.copy(amount = newAmount)
                                            sections[sIndex] = section.copy(ingredients = newIngredients)
                                        },
                                        modifier = Modifier.width(80.dp),
                                        label = { Text("Ilość") },
                                        suffix = { Text("g") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    IconButton(onClick = {
                                        val newIngredients = section.ingredients.toMutableList()
                                        newIngredients.removeAt(iIndex)
                                        sections[sIndex] = section.copy(ingredients = newIngredients)
                                    }) {
                                        Icon(Icons.Default.RemoveCircleOutline, "Usuń składnik")
                                    }
                                }
                            }

                            TextButton(
                                onClick = { showProductSearch = sIndex },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Search, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dodaj składnik")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (showProductSearch != null) {
            AlertDialog(
                onDismissRequest = { showProductSearch = null },
                title = { Text("Dodaj składnik do sekcji") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = searchProductQuery,
                            onValueChange = { 
                                searchProductQuery = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Szukaj produktu...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(filteredProducts) { product ->
                                ListItem(
                                    headlineContent = { Text(product.name) },
                                    supportingContent = { Text("${product.calories.toInt()} kcal/100g") },
                                    modifier = Modifier.clickable {
                                        val sIdx = showProductSearch!!
                                        val newIngredients = sections[sIdx].ingredients.toMutableList()
                                        newIngredients.add(RecipeIngredientDto(product = product, amount = 100.0))
                                        sections[sIdx] = sections[sIdx].copy(ingredients = newIngredients)
                                        showProductSearch = null
                                        searchProductQuery = ""
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showProductSearch = null }) { Text("Zamknij") } }
            )
        }
    }
}

@Composable
fun EditProductDialog(
    product: ProductDto,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ProductDto) -> Unit
) {
    var name by remember(product) { mutableStateOf(product.name) }
    var barcode by remember(product) { mutableStateOf(product.barcode ?: "") }
    var calories by remember(product) { mutableStateOf(if (isNew && product.calories == 0.0) "" else product.calories.toString()) }
    var protein by remember(product) { mutableStateOf(if (isNew && product.protein == 0.0) "" else product.protein.toString()) }
    var fat by remember(product) { mutableStateOf(if (isNew && product.fat == 0.0) "" else product.fat.toString()) }
    var carbs by remember(product) { mutableStateOf(if (isNew && product.carbohydrates == 0.0) "" else product.carbohydrates.toString()) }
    
    var unitName by remember(product) { mutableStateOf(product.unitName ?: "sztuka") }
    var unitWeightG by remember(product) { mutableStateOf(product.unitWeightG?.let { if(it == 0.0) "" else it.toString() } ?: "") }
    
    val units = listOf("sztuka", "porcja", "opakowanie", "szklanka", "łyżka")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Dodaj własny produkt" else "Edytuj produkt") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa produktu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Kod kreskowy (opcjonalnie)") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("Jednostka opcjonalna", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = unitName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jednostka") },
                            trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            units.forEach { unit ->
                                DropdownMenuItem(text = { Text(unit) }, onClick = { unitName = unit; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = unitWeightG,
                        onValueChange = { unitWeightG = it },
                        label = { Text("Waga (g)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Makroskładniki
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Wartości na 100g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Kcal") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("B") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("T") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("W") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = product.copy(
                        name = name,
                        calories = calories.toDoubleOrNull() ?: 0.0,
                        protein = protein.toDoubleOrNull() ?: 0.0,
                        fat = fat.toDoubleOrNull() ?: 0.0,
                        carbohydrates = carbs.toDoubleOrNull() ?: 0.0,
                        barcode = barcode.ifBlank { null },
                        unitName = unitName,
                        unitWeightG = unitWeightG.toDoubleOrNull()
                    )
                    onConfirm(updated)
                },
                enabled = name.isNotBlank() && calories.isNotBlank() && protein.isNotBlank() && fat.isNotBlank() && carbs.isNotBlank()
            ) {
                Text(if (isNew) "Dodaj" else "Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}