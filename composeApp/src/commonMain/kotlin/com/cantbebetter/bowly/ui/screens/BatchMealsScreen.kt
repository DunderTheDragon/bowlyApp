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
                userProfile?.let {
                    viewModel.updateUserProfile(it.copy(showBatchOnboarding = false))
                }
                showOnboarding = false
            }
        )
    }

    if (selectedMeal != null) {
        TakePortionDialog(
            meal = selectedMeal!!,
            onDismiss = { selectedMeal = null },
            onConfirm = { segmentId, weight, mealType, timestamp ->
                val date = Clock.formatToApiDate(timestamp)
                viewModel.consumePortion(
                    ConsumePortionRequest(segmentId, weight, mealType),
                    date
                )
                selectedMeal = null
            }
        )
    }

    if (showCreateDialog) {
        CreateBatchMealDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                viewModel.createBatchMeal(request)
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
    initialMeal: BatchMealDto? = null,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (CreateBatchMealRequest) -> Unit
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var mealName by remember { mutableStateOf(initialMeal?.name ?: "") }
    
    val initialSections = remember {
        val list = mutableListOf<SectionData>()
        if (initialMeal != null) {
            initialMeal.segments.forEach { segment ->
                list.add(SectionData(
                    id = segment.id ?: Clock.uniqueId(),
                    name = segment.name,
                    products = listOf(ProductData(segment.product, segment.initialWeightG))
                ))
            }
        } else {
            list.add(SectionData(id = "s_${Clock.uniqueId()}", name = "Główna część"))
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
                val title = if (initialMeal != null) "Edytuj patelnię" else "Nowa patelnia"
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    
                    val request = CreateBatchMealRequest(
                        name = mealName,
                        segments = sections.map { section ->
                            val product = section.products.firstOrNull()?.product
                            CreateBatchMealSegmentRequest(
                                name = section.name.ifBlank { mealName },
                                productId = product?.id ?: "",
                                initialWeightG = section.products.sumOf { it.weightG }
                            )
                        }
                    )
                    onConfirm(request)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = mealName.isNotBlank() && sections.any { it.products.isNotEmpty() }
            ) {
                val confirmText = if (initialMeal != null) "Zapisz zmiany" else "Utwórz patelnię"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(confirmText)
                    Text("Łącznie: ${totalWeight.toInt()}g", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    )

    if (activeSectionIdForSearch != null) {
        val currentActiveId = activeSectionIdForSearch 
        SimpleProductSearchDialog(
            viewModel = viewModel,
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
    meal: BatchMealDto,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Long) -> Unit
) {
    val portions = remember { mutableStateMapOf<String, String>().apply {
        meal.segments.forEach { it.id?.let { id -> put(id, "") } }
    }}
    
    var selectedTimestamp by remember { mutableStateOf(Clock.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val mealTypes = listOf("Śniadanie", "Obiad", "Kolacja", "Inne")
    var selectedMealType by remember { mutableStateOf("Obiad") }
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
                    val segmentId = segment.id ?: return@forEach
                    val weightStr = portions[segmentId] ?: ""
                    val weight = weightStr.toDoubleOrNull() ?: 0.0
                    val isError = weightStr.isNotBlank() && (weight < 0 || weight > segment.currentWeightG)
                    
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(segment.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("pozostało: ${segment.currentWeightG.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { portions[segmentId] = it },
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
                    meal.segments.forEach { segment ->
                        val segmentId = segment.id ?: return@forEach
                        val weight = portions[segmentId]?.toDoubleOrNull() ?: 0.0
                        if (weight > 0) {
                            onConfirm(segmentId, weight, selectedMealType, selectedTimestamp)
                        }
                    }
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

@Composable
fun MealCard(meal: BatchMealDto, onTakePortion: () -> Unit) {
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
fun SimpleProductSearchDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onProductSelected: (ProductDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var barcodeToPreFill by remember { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    val filteredProducts by viewModel.searchResults.collectAsState()

    LaunchedEffect(query) {
        viewModel.searchProducts(query)
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { 
                showAddDialog = false
                barcodeToPreFill = null
            },
            onConfirm = { newProduct ->
                onProductSelected(newProduct)
                showAddDialog = false
            }
        )
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BarcodeScannerView(
                onBarcodeDetected = { code ->
                    // For now, just search with barcode in query
                    query = code
                    showScanner = false
                },
                onClose = { showScanner = false }
            )
        }
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
                    placeholder = { Text("Nazwa lub kod kreskowy...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, "Skanuj", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (filteredProducts.isEmpty() && query.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nie znaleziono produktu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text("Dodaj nowy produkt")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(filteredProducts) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                supportingContent = { 
                                    Text("${product.calories.toInt()} kcal/100g | B: ${product.protein} T: ${product.fat} W: ${product.carbohydrates}")
                                },
                                modifier = Modifier.clickable { onProductSelected(product) }
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
                                Text("Dodaj produkt spoza listy")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (ProductDto) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Nowy produkt") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa produktu") }, modifier = Modifier.fillMaxWidth())
                
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
                        barcode = null,
                        source = "USER"
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
