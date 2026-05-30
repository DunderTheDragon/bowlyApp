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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cantbebetter.bowly.ui.components.BarcodeScannerView

import com.cantbebetter.bowly.data.network.*
import com.cantbebetter.bowly.models.MealTypeMapper
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

data class SectionData(
    val id: String,
    val name: String,
    val products: List<ProductData> = emptyList()
)

data class ProductData(
    val product: ProductDto,
    val weightG: Double
)

@Composable
fun BatchMealsScreen(
    viewModel: MainViewModel
) {
    val activeMeals by viewModel.activeBatchMeals.collectAsState()
    var selectedMeal by remember { mutableStateOf<BatchMealDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var personalizeRecipe by remember { mutableStateOf<com.cantbebetter.bowly.data.network.RecipeDto?>(null) }
    val userProfile by viewModel.userProfile.collectAsState()
    var showOnboarding by remember { mutableStateOf(userProfile?.showBatchOnboarding ?: false) }

    // Synchronizacja showOnboarding z profilem
    LaunchedEffect(userProfile?.showBatchOnboarding) {
        showOnboarding = userProfile?.showBatchOnboarding ?: false
    }

    LaunchedEffect(Unit) {
        viewModel.loadActiveBatchMeals()
        viewModel.loadUserProfile()
    }

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
                item {
                    val totalRemainingG = activeMeals.sumOf { it.totalCurrentWeightG() }
                    val totalInitialG = activeMeals.sumOf { it.totalInitialWeightG() }
                    val totalKcalLeft = activeMeals.sumOf { it.totalRemainingKcal() }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Łącznie na patelniach",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${totalRemainingG.toInt()}g pozostało z ${totalInitialG.toInt()}g · ${totalKcalLeft.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (totalInitialG > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (totalRemainingG / totalInitialG).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                )
                            }
                        }
                    }
                }
                items(activeMeals) { meal ->
                    MealCard(
                        meal = meal,
                        onTakePortion = { selectedMeal = meal },
                        onDelete = { viewModel.deleteBatchMeal(meal.id) }
                    )
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
                userProfile?.let {
                    viewModel.updateUserProfile(it.copy(showBatchOnboarding = false))
                }
                showOnboarding = false
            }
        )
    }

    if (selectedMeal != null) {
        Dialog(
            onDismissRequest = { selectedMeal = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                BatchMealAddDetail(
                    viewModel = viewModel,
                    batchMeal = selectedMeal!!,
                    initialMealType = "Obiad",
                    initialDate = Clock.formatToApiDate(Clock.now()),
                    isPredefined = false,
                    onBack = { selectedMeal = null },
                    onConfirm = { selectedMeal = null }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateBatchMealDialog(
            viewModel = viewModel,
            initialRecipe = personalizeRecipe,
            onDismiss = {
                showCreateDialog = false
                personalizeRecipe = null
            },
            onConfirm = { request ->
                viewModel.createBatchMeal(request) {
                    showCreateDialog = false
                    personalizeRecipe = null
                }
            },
            onCreateFromRecipe = { recipe ->
                viewModel.createBatchFromRecipe(recipe) {
                    showCreateDialog = false
                }
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
    initialMeal: BatchMealDto? = null,
    initialRecipe: com.cantbebetter.bowly.data.network.RecipeDto? = null,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (CreateBatchMealRequest) -> Unit,
    onCreateFromRecipe: (com.cantbebetter.bowly.data.network.RecipeDto) -> Unit
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var creationMode by remember(initialRecipe) {
        mutableStateOf(if (initialRecipe != null) "NEW" else "NEW")
    }
    var saveAsRecipe by remember { mutableStateOf(false) }
    var mealName by remember(initialRecipe, initialMeal) {
        mutableStateOf(initialRecipe?.name ?: initialMeal?.name ?: "")
    }

    val initialSections = remember(initialRecipe, initialMeal) {
        val list = mutableListOf<SectionData>()
        when {
            initialRecipe != null -> list.addAll(initialRecipe.toSectionDataList())
            initialMeal != null -> {
                initialMeal.segments.forEach { segment ->
                    segment.product?.let {
                        list.add(
                            SectionData(
                                id = segment.id.toString(),
                                name = segment.name,
                                products = listOf(ProductData(it, segment.initialWeightG))
                            )
                        )
                    }
                }
            }
            else -> list.add(SectionData(id = "s_${Clock.uniqueId()}", name = "Główna część"))
        }
        list
    }

    val sections = remember { mutableStateListOf<SectionData>().apply { addAll(initialSections) } }
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
                val title = when {
                    initialRecipe != null -> "Personalizuj patelnię"
                    initialMeal != null -> "Edytuj patelnię"
                    else -> "Nowa patelnia"
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (initialRecipe == null && initialMeal == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = creationMode == "NEW",
                            onClick = { creationMode = "NEW" },
                            label = { Text("Nowa patelnia") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = creationMode == "RECIPE",
                            onClick = { creationMode = "RECIPE" },
                            label = { Text("Mój przepis") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (creationMode == "RECIPE" && initialRecipe == null && initialMeal == null) {
                    RecipePickerContent(
                        viewModel = viewModel,
                        onRecipeSelected = onCreateFromRecipe,
                        onPersonalize = { recipe ->
                            mealName = recipe.name
                            sections.clear()
                            sections.addAll(recipe.toSectionDataList())
                            creationMode = "NEW"
                        }
                    )
                } else {
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
                            modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
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

                if (initialRecipe == null && initialMeal == null && creationMode == "NEW") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveAsRecipe,
                            onCheckedChange = { saveAsRecipe = it }
                        )
                        Text(
                            "Utwórz przepis",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { saveAsRecipe = !saveAsRecipe }
                        )
                    }
                }
                }
            }
        },
        confirmButton = {
            if (creationMode != "RECIPE" || initialRecipe != null || initialMeal != null) {
            Button(
                onClick = {
                    keyboardController?.hide()

                    val shouldSaveRecipe = saveAsRecipe && initialRecipe == null && initialMeal == null
                    val request = CreateBatchMealRequest(
                        name = mealName,
                        saveAsRecipe = shouldSaveRecipe,
                        recipeSections = if (shouldSaveRecipe) sections.toRecipeSectionsForSave() else null,
                        segments = sections.mapNotNull { section ->
                            if (section.products.isEmpty()) return@mapNotNull null
                            val totalWeight = section.products.sumOf { it.weightG }
                            if (totalWeight <= 0) return@mapNotNull null
                            val primaryProduct = section.products.first().product
                            val macros = sectionMacrosFromProducts(section.products)
                            CreateBatchMealSegmentRequest(
                                name = section.name.ifBlank { mealName },
                                productId = primaryProduct.id,
                                product = primaryProduct,
                                products = section.products.map { it.product },
                                initialWeightG = totalWeight,
                                totalKcal = macros.totalKcal,
                                totalProtein = macros.totalProtein,
                                totalFat = macros.totalFat,
                                totalCarbs = macros.totalCarbs
                            )
                        }
                    )
                    onConfirm(request)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = mealName.isNotBlank() && sections.any { it.products.isNotEmpty() && it.products.sumOf { p -> p.weightG } > 0 }
            ) {
                val confirmText = if (initialMeal != null) "Zapisz zmiany" else "Utwórz patelnię"
                val totalKcal = sections.sumOf { sectionMacrosFromProducts(it.products).totalKcal }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(confirmText)
                    Text(
                        "Łącznie: ${totalWeight.toInt()}g · ${totalKcal.toInt()} kcal",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            }
        }
    )

    if (activeSectionIdForSearch != null) {
        val currentActiveId = activeSectionIdForSearch 
        Dialog(
            onDismissRequest = { activeSectionIdForSearch = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ProductSearchScreen(
                    viewModel = viewModel,
                    onBack = { activeSectionIdForSearch = null },
                    onProductSelected = { product ->
                        viewModel.cacheProduct(product) { saved ->
                            val sectionIndex = sections.indexOfFirst { it.id == currentActiveId }
                            if (sectionIndex != -1) {
                                val section = sections[sectionIndex]
                                val updatedProducts = section.products.toMutableList()
                                updatedProducts.add(ProductData(product = saved, weightG = 0.0))
                                sections[sectionIndex] = section.copy(products = updatedProducts)
                            }
                            activeSectionIdForSearch = null
                        }
                    }
                )
            }
        }
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

@Composable
fun MealCard(
    meal: BatchMealDto,
    onTakePortion: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Usuń patelnię") },
            text = {
                Text(
                    "Czy na pewno chcesz usunąć „${meal.name}”? " +
                        "Pozostała zawartość (${meal.totalCurrentWeightG().toInt()}g) zostanie odrzucona. " +
                        "Wpisy w dzienniku z tej patelni pozostaną."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    meal.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Usuń patelnię",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${meal.totalCurrentWeightG().toInt()}g",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "z ${meal.totalInitialWeightG().toInt()}g · ${meal.totalRemainingKcal().toInt()} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { meal.overallProgress() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            meal.segments.forEach { segment ->
                BatchMealSegmentRow(segment)
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
fun BatchMealSegmentRow(segment: BatchMealSegmentDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(segment.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                segment.product?.let {
                    Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${segment.currentWeightG.toInt()}g / ${segment.initialWeightG.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${segment.remainingKcal().toInt()} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { segment.remainingProgress() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        )
    }
}

@Composable
fun ProductSearchScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProductSelected: (ProductDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    val barcodeToPrefill by viewModel.barcodeToPrefill.collectAsState()
    val displayProducts by viewModel.displaySearchResults.collectAsState()
    val error by viewModel.error.collectAsState()
    val isSearching by viewModel.isSearchingProducts.collectAsState()

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
                query = ""
                viewModel.clearProductSearch()
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
            }
            Text(
                text = "Dodaj składnik",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
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

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (error != null) {
            Text(
                text = "Błąd: $error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                            query = ""
                            viewModel.clearProductSearch()
                        }
                    }
                )
            }
            if (!isSearching) {
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
}

@Composable
fun AddProductDialog(
    barcodeToPreFill: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (ProductDto) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var barcode by remember(barcodeToPreFill) { mutableStateOf(barcodeToPreFill ?: "") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var unitName by remember { mutableStateOf("") }
    var unitWeightG by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Nowy produkt") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa produktu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Kod kreskowy (opcjonalnie)") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                
                Text(
                    "Opcjonalnie: ułatwienie dodawania w porcjach",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitName, 
                        onValueChange = { unitName = it }, 
                        label = { Text("Nazwa porcji (np. plaster, sztuka)") }, 
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitWeightG, 
                        onValueChange = { unitWeightG = it }, 
                        label = { Text("Waga (g)") }, 
                        modifier = Modifier.weight(1f), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            val macroComplete = calories.isNotBlank() && protein.isNotBlank() && fat.isNotBlank() && carbs.isNotBlank()
            Button(
                onClick = {
                    val product = ProductDto(
                        id = null,
                        name = name,
                        calories = calories.toDoubleOrNull() ?: 0.0,
                        protein = protein.toDoubleOrNull() ?: 0.0,
                        fat = fat.toDoubleOrNull() ?: 0.0,
                        carbohydrates = carbs.toDoubleOrNull() ?: 0.0,
                        barcode = barcode.ifBlank { null },
                        source = "USER",
                        unitName = unitName.takeIf { it.isNotBlank() },
                        unitWeightG = unitWeightG.toDoubleOrNull()
                    )
                    onConfirm(product)
                },
                enabled = name.isNotBlank() && macroComplete
            ) {
                Text("Dodaj produkt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}