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
import com.cantbebetter.bowly.data.MockData
import com.cantbebetter.bowly.models.MicroElements
import com.cantbebetter.bowly.models.Product
import com.cantbebetter.bowly.models.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductsScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Produkty", "Przepisy")

    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Recipe editing state
    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }

    val filteredProducts = MockData.products.filter {
        it.source == "USER" && (it.name.contains(searchQuery, ignoreCase = true) || (it.barcode?.contains(searchQuery) == true))
    }
    
    val filteredRecipes = MockData.recipes.filter {
        it.name.contains(searchQuery, ignoreCase = true)
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
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj produkt")
                }
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
                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Brak własnych produktów", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    Text("${product.calories.toInt()} kcal/100g | B: ${product.protein} T: ${product.fat} W: ${product.carbs}")
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
                if (filteredRecipes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Brak przepisów", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredRecipes) { recipe ->
                            val totalWeight = recipe.sections.sumOf { it.ingredients.sumOf { i -> i.weightG } }
                            val totalCalories = recipe.sections.sumOf { it.ingredients.sumOf { i -> (i.weightG / 100.0) * i.product.calories } }
                            val totalProtein = recipe.sections.sumOf { it.ingredients.sumOf { i -> (i.weightG / 100.0) * i.product.protein } }
                            val totalFat = recipe.sections.sumOf { it.ingredients.sumOf { i -> (i.weightG / 100.0) * i.product.fat } }
                            val totalCarbs = recipe.sections.sumOf { it.ingredients.sumOf { i -> (i.weightG / 100.0) * i.product.carbs } }
                            
                            ListItem(
                                headlineContent = { Text(recipe.name) },
                                supportingContent = {
                                    Column {
                                        Text("Waga: ${totalWeight.toInt()}g | ${totalCalories.toInt()} kcal")
                                        Text("B: ${totalProtein.toInt()} T: ${totalFat.toInt()} W: ${totalCarbs.toInt()}", style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { editingRecipe = recipe }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edytuj")
                                        }
                                        IconButton(onClick = { recipeToDelete = recipe }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { 
                                    // Clicking a recipe in MyProducts could either edit it or start a new BatchMeal
                                    editingRecipe = recipe 
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        EditProductDialog(
            product = Product(
                id = "user_p_${Clock.uniqueId()}",
                name = "",
                calories = 0.0,
                protein = 0.0,
                fat = 0.0,
                carbs = 0.0,
                unitName = "sztuka",
                unitWeightG = 100.0,
                source = "USER"
            ),
            isNew = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { newProduct ->
                MockData.products.add(0, newProduct)
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
                val index = MockData.products.indexOfFirst { it.id == updatedProduct.id }
                if (index != -1) {
                    MockData.products[index] = updatedProduct
                }
                editingProduct = null
            }
        )
    }
    
    if (editingRecipe != null) {
        // Reuse CreateBatchMealDialog for editing recipe or starting a new batch from it
        CreateBatchMealDialog(
            initialRecipe = editingRecipe,
            isEditingRecipe = true,
            onDismiss = { editingRecipe = null },
            onConfirm = { /* Not used in isEditingRecipe mode */ },
            onRecipeConfirm = { updatedRecipe ->
                val index = MockData.recipes.indexOfFirst { it.id == updatedRecipe.id }
                if (index != -1) {
                    MockData.recipes[index] = updatedRecipe
                }
                editingRecipe = null
            }
        )
    }

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Usuń przepis") },
            text = { Text("Czy na pewno chcesz trwale usunąć przepis \"${recipeToDelete?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        MockData.recipes.remove(recipeToDelete)
                        recipeToDelete = null
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun EditProductDialog(
    product: Product,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var barcode by remember { mutableStateOf(product.barcode ?: "") }
    var calories by remember { mutableStateOf(if (isNew && product.calories == 0.0) "" else product.calories.toString()) }
    var protein by remember { mutableStateOf(if (isNew && product.protein == 0.0) "" else product.protein.toString()) }
    var fat by remember { mutableStateOf(if (isNew && product.fat == 0.0) "" else product.fat.toString()) }
    var carbs by remember { mutableStateOf(if (isNew && product.carbs == 0.0) "" else product.carbs.toString()) }
    
    var unitName by remember { mutableStateOf(product.unitName) }
    var unitWeightG by remember { mutableStateOf(product.unitWeightG.toString()) }

    var showAdvanced by remember { mutableStateOf(false) }
    var fiber by remember { mutableStateOf(product.micros.fiber?.toString() ?: "") }
    var sugar by remember { mutableStateOf(product.micros.sugar?.toString() ?: "") }
    var salt by remember { mutableStateOf(product.micros.salt?.toString() ?: "") }
    var saturatedFat by remember { mutableStateOf(product.micros.saturatedFat?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Dodaj własny produkt" else "Edytuj produkt") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa produktu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Kod kreskowy (opcjonalnie)") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Makroskładniki w ramce
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Surface(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-10).dp)
                            .zIndex(1f),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            " na 100g* ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                Spacer(modifier = Modifier.height(16.dp))
                Text("Domyślna jednostka:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = unitName, onValueChange = { unitName = it }, label = { Text("Nazwa (np. sztuka)") }, modifier = Modifier.weight(1.5f))
                    OutlinedTextField(value = unitWeightG, onValueChange = { unitWeightG = it }, label = { Text("Waga (g)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Ukryj mikroelementy" else "Pokaż mikroelementy")
                    Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }

                if (showAdvanced) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Surface(
                            modifier = Modifier
                                .offset(x = 12.dp, y = (-10).dp)
                                .zIndex(1f),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                " mikro / 100g ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = fiber, onValueChange = { fiber = it }, label = { Text("Błonnik") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = sugar, onValueChange = { sugar = it }, label = { Text("Cukry") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = salt, onValueChange = { salt = it }, label = { Text("Sól") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = saturatedFat, onValueChange = { saturatedFat = it }, label = { Text("Nasycone") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                        }
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
                        carbs = carbs.toDoubleOrNull() ?: 0.0,
                        barcode = barcode.ifBlank { null },
                        unitName = unitName,
                        unitWeightG = unitWeightG.toDoubleOrNull() ?: 100.0,
                        micros = MicroElements(
                            fiber = fiber.toDoubleOrNull(),
                            sugar = sugar.toDoubleOrNull(),
                            salt = salt.toDoubleOrNull(),
                            saturatedFat = saturatedFat.toDoubleOrNull()
                        )
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
