package com.cantbebetter.bowly.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cantbebetter.bowly.data.SettingsManager
import com.cantbebetter.bowly.data.network.*
import com.cantbebetter.bowly.models.DailyStats
import com.cantbebetter.bowly.models.MealTypeMapper
import com.cantbebetter.bowly.ui.screens.isLikelyBarcode
import com.cantbebetter.bowly.ui.screens.toCreateBatchMealRequest
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AppState {
    object Loading : AppState()
    object EnterServerAddress : AppState()
    object LoginRequired : AppState()
    object Authenticated : AppState()
}

class MainViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    private val _uiState = MutableStateFlow<AppState>(AppState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _registrationSuccess = MutableStateFlow<Boolean>(false)
    val registrationSuccess = _registrationSuccess.asStateFlow()

    private val _adminKeys = MutableStateFlow<AdminKeysDto?>(null)
    val adminKeys = _adminKeys.asStateFlow()

    private val _users = MutableStateFlow<List<UserDto>>(emptyList())
    val users = _users.asStateFlow()

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    val userProfile = _userProfile.asStateFlow()

    private var profileUpdateJob: Job? = null

    private val _dailyStats = MutableStateFlow<DailyStats?>(null)
    val dailyStats = _dailyStats.asStateFlow()

    private val _dailySummary = MutableStateFlow<DailySummaryDto?>(null)
    val dailySummary = _dailySummary.asStateFlow()

    private val _activeBatchMeals = MutableStateFlow<List<BatchMealDto>>(emptyList())
    val activeBatchMeals = _activeBatchMeals.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    private val _externalSearchResults = MutableStateFlow<List<ProductDto>>(emptyList())
    private val _localProducts = MutableStateFlow<List<ProductDto>>(emptyList())
    val localProducts = _localProducts.asStateFlow()
    private var externalSearchJob: Job? = null

    val displaySearchResults = combine(
        _localProducts,
        _externalSearchResults,
        _productSearchQuery
    ) { local, external, query ->
        mergeSearchResults(local, external, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isSearchingProducts = MutableStateFlow<Boolean>(false)
    val isSearchingProducts = _isSearchingProducts.asStateFlow()

    private val _recipeSearchResults = MutableStateFlow<List<RecipeDto>>(emptyList())
    val recipeSearchResults = _recipeSearchResults.asStateFlow()

    private val _allRecipes = MutableStateFlow<List<RecipeDto>>(emptyList())
    val allRecipes = _allRecipes.asStateFlow()

    private val _barcodeToPrefill = MutableStateFlow<String?>(null)
    val barcodeToPrefill = _barcodeToPrefill.asStateFlow()

    private val _scannedProduct = MutableStateFlow<ProductDto?>(null)
    val scannedProduct = _scannedProduct.asStateFlow()

    private val _containers = MutableStateFlow<List<WeighingContainerDto>>(emptyList())
    val containers = _containers.asStateFlow()

    private var apiService: ApiService? = null

    init {
        checkStatus()
    }

    fun setServerAddress(url: String) {
        if (url.isBlank()) {
            _error.value = "Adres serwera nie może być pusty"
            return
        }
        var sanitizedUrl = url.trim().removeSuffix("/")
        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
            sanitizedUrl = "http://$sanitizedUrl"
        }
        settingsManager.baseUrl = sanitizedUrl
        _error.value = null
        checkStatus()
    }

    fun openChangeServerAddress() {
        _error.value = null
        _uiState.value = AppState.EnterServerAddress
    }

    fun cancelChangeServerAddress() {
        _error.value = null
        if (!settingsManager.baseUrl.isNullOrBlank()) {
            _uiState.value = AppState.LoginRequired
        }
    }

    val currentServerAddress: String?
        get() = settingsManager.baseUrl?.takeIf { it.isNotBlank() }

    fun checkStatus() {
        val baseUrl = settingsManager.baseUrl
        if (baseUrl.isNullOrBlank()) {
            _uiState.value = AppState.EnterServerAddress
            return
        }

        _uiState.value = AppState.Loading
        apiService = ApiService(baseUrl, settingsManager.token)
        viewModelScope.launch {
            try {
                val status = apiService?.getStatus()
                if (settingsManager.token.isNullOrBlank()) {
                    _uiState.value = AppState.LoginRequired
                } else {
                    try {
                        // Weryfikacja tokena przez pobranie profilu
                        val profile = apiService?.getUserProfile()
                        if (profile != null) {
                            _userProfile.value = profile
                            _dailyStats.value = calculateDailyStats(profile)
                            _uiState.value = AppState.Authenticated
                        } else {
                            _uiState.value = AppState.LoginRequired
                        }
                    } catch (e: Exception) {
                        // Token nieważny lub błąd połączenia
                        _uiState.value = AppState.LoginRequired
                    }
                }
            } catch (e: Exception) {
                _error.value = "Nie można połączyć się z serwerem: ${e.message}"
                _uiState.value = AppState.EnterServerAddress
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            try {
                val response = apiService?.login(request)
                if (response != null) {
                    settingsManager.token = response.token
                    settingsManager.username = response.username ?: request.username
                    settingsManager.role = response.role
                    apiService = ApiService(settingsManager.baseUrl!!, response.token)
                    val profile = apiService?.getUserProfile()
                    if (profile != null) {
                        _userProfile.value = profile
                        _dailyStats.value = calculateDailyStats(profile)
                    }
                    _uiState.value = AppState.Authenticated
                }
            } catch (e: Exception) {
                _error.value = "Błąd logowania: ${e.message}"
            }
        }
    }

    fun loadAdminData() {
        viewModelScope.launch {
            try {
                _adminKeys.value = apiService?.getAdminKeys()
                _users.value = apiService?.getUsers() ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Błąd pobierania danych: ${e.message}"
            }
        }
    }

    fun saveAdminKeys(keys: AdminKeysDto) {
        viewModelScope.launch {
            try {
                apiService?.saveAdminKeys(keys)
                _adminKeys.value = keys
            } catch (e: Exception) {
                _error.value = "Błąd zapisu kluczy: ${e.message}"
            }
        }
    }

    fun registerUser(request: RegisterRequest) {
        viewModelScope.launch {
            try {
                val success = apiService?.registerUser(request) ?: false
                if (success) {
                    _registrationSuccess.value = true
                }
            } catch (e: Exception) {
                _error.value = "Błąd rejestracji użytkownika: ${e.message}"
            }
        }
    }

    fun registrationSuccessHandled() {
        _registrationSuccess.value = false
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            try {
                val success = apiService?.deleteUser(userId) ?: false
                if (success) {
                    _users.value = apiService?.getUsers() ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania użytkownika: ${e.message}"
            }
        }
    }

    // User Profile
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = apiService?.getUserProfile()
                _userProfile.value = profile
                profile?.let { _dailyStats.value = calculateDailyStats(it) }
            } catch (e: Exception) {
                _error.value = "Błąd pobierania profilu: ${e.message}"
            }
        }
    }

    fun updateUserProfile(user: UserDto) {
        _userProfile.value = user
        _dailyStats.value = calculateDailyStats(user)

        profileUpdateJob?.cancel()
        profileUpdateJob = viewModelScope.launch {
            delay(400)
            try {
                val saved = apiService?.updateUserProfile(user)
                if (saved != null) {
                    _userProfile.value = saved
                    _dailyStats.value = calculateDailyStats(saved)
                }
            } catch (e: Exception) {
                _error.value = "Błąd aktualizacji profilu: ${e.message}"
                loadUserProfile()
            }
        }
    }

    fun calculateDailyStats(user: UserDto): DailyStats {
        val bmr = if (user.gender == "MALE") {
            (10 * user.weightKg) + (6.25 * user.heightCm) - (5 * user.age) + 5
        } else {
            (10 * user.weightKg) + (6.25 * user.heightCm) - (5 * user.age) - 161
        }

        val tdee = bmr * user.activityLevel
        val calorieOffset = (user.weeklyChangeRateKg * 7700.0) / 7.0

        val targetCalories = if (user.targetWeightKg < user.weightKg) {
            tdee - calorieOffset
        } else if (user.targetWeightKg > user.weightKg) {
            tdee + calorieOffset
        } else {
            tdee
        }

        val pRatio = user.proteinRatio / 100.0
        val fRatio = user.fatRatio / 100.0
        val cRatio = user.carbsRatio / 100.0

        return DailyStats(
            consumedCalories = 0.0,
            targetCalories = targetCalories,
            protein = 0.0,
            targetProtein = (targetCalories * pRatio) / 4.0,
            fat = 0.0,
            targetFat = (targetCalories * fRatio) / 9.0,
            carbs = 0.0,
            targetCarbs = (targetCalories * cRatio) / 4.0
        )
    }

    // Diary & Summary
    fun loadDailySummary(date: String) {
        viewModelScope.launch {
            try {
                _dailySummary.value = apiService?.getDailySummary(date)
            } catch (e: Exception) {
                _error.value = "Błąd pobierania podsumowania dnia: ${e.message}"
            }
        }
    }

    fun addWorkout(name: String, caloriesBurned: Double, date: String) {
        viewModelScope.launch {
            try {
                apiService?.addWorkout(
                    CreateWorkoutActivityRequest(
                        name = name,
                        caloriesBurned = caloriesBurned,
                        activityDate = date
                    )
                )
                loadDailySummary(date)
            } catch (e: Exception) {
                _error.value = "Błąd dodawania treningu: ${e.message}"
            }
        }
    }

    fun deleteWorkout(id: Long, date: String) {
        viewModelScope.launch {
            try {
                if (apiService?.deleteWorkout(id) == true) {
                    loadDailySummary(date)
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania treningu: ${e.message}"
            }
        }
    }

    // Products & Recipes
    fun onProductSearchQueryChanged(query: String) {
        _productSearchQuery.value = query
        externalSearchJob?.cancel()

        if (query.isEmpty()) {
            _externalSearchResults.value = emptyList()
            _isSearchingProducts.value = false
            return
        }

        if (isLikelyBarcode(query)) {
            searchByBarcode(query)
            return
        }

        if (query.length < MIN_EXTERNAL_QUERY_LENGTH) {
            _externalSearchResults.value = emptyList()
            _isSearchingProducts.value = false
            return
        }

        externalSearchJob = viewModelScope.launch {
            delay(EXTERNAL_SEARCH_DEBOUNCE_MS)
            if (_productSearchQuery.value != query) return@launch

            _isSearchingProducts.value = true
            try {
                val results = apiService?.searchExternalProducts(query) ?: emptyList()
                if (_productSearchQuery.value == query) {
                    _externalSearchResults.value = results
                }
            } catch (e: Exception) {
                if (_productSearchQuery.value == query) {
                    _error.value = "Błąd wyszukiwania API: ${e.message}"
                }
            } finally {
                if (_productSearchQuery.value == query) {
                    _isSearchingProducts.value = false
                }
            }
        }
    }

    private fun searchByBarcode(barcode: String) {
        _externalSearchResults.value = emptyList()

        val inLocal = _localProducts.value.any { it.barcode == barcode.trim() }
        if (inLocal) {
            _isSearchingProducts.value = false
            return
        }

        externalSearchJob = viewModelScope.launch {
            delay(EXTERNAL_SEARCH_DEBOUNCE_MS)
            if (_productSearchQuery.value != barcode) return@launch

            _isSearchingProducts.value = true
            try {
                val product = apiService?.getProductByBarcode(barcode.trim())
                if (product != null && _productSearchQuery.value == barcode) {
                    _externalSearchResults.value = listOf(product)
                }
            } catch (e: Exception) {
                if (_productSearchQuery.value == barcode) {
                    _error.value = "Błąd wyszukiwania kodu kreskowego: ${e.message}"
                }
            } finally {
                if (_productSearchQuery.value == barcode) {
                    _isSearchingProducts.value = false
                }
            }
        }
    }

    suspend fun ensureProductSaved(product: ProductDto): ProductDto {
        val service = apiService ?: return product
        return try {
            service.saveLocalProduct(product)
        } catch (_: Exception) {
            if (product.id != null) {
                service.saveLocalProduct(product.copy(id = null))
            } else {
                throw IllegalStateException("Nie udało się zapisać produktu: ${product.name}")
            }
        }
    }

    private suspend fun prepareBatchMealRequest(request: CreateBatchMealRequest): CreateBatchMealRequest {
        val segments = request.segments.map { segment ->
            val savedProducts = segment.products.orEmpty().map { ensureProductSaved(it) }
            val primary = savedProducts.firstOrNull()
                ?: segment.product?.let { ensureProductSaved(it) }
            segment.copy(
                productId = primary?.id,
                product = primary,
                products = savedProducts.ifEmpty { primary?.let { listOf(it) } }
            )
        }
        val recipeSections = request.recipeSections.map { section ->
            section.copy(
                ingredients = section.ingredients.map { ingredient ->
                    val saved = ingredient.product?.let { ensureProductSaved(it) }
                    ingredient.copy(
                        productId = saved?.id?.toLongOrNull() ?: ingredient.productId,
                        product = saved ?: ingredient.product
                    )
                }
            )
        }
        if (segments.isEmpty()) {
            throw IllegalArgumentException("Patelnia musi mieć co najmniej jedną sekcję ze składnikami")
        }
        return request.copy(segments = segments, recipeSections = recipeSections)
    }

    private fun formatUserFacingError(e: Exception): String {
        val clientError = e as? ClientRequestException
        if (clientError != null) {
            val status = clientError.response.status.value
            val raw = clientError.message.orEmpty()
            val serverMessage = extractServerErrorMessage(raw)
            return when (status) {
                400 -> serverMessage ?: "Serwer odrzucił dane patelni (400). Zaktualizuj backend i sprawdź składniki."
                else -> serverMessage ?: "Błąd serwera ($status)"
            }
        }
        return when (e) {
            is IllegalArgumentException, is IllegalStateException -> e.message ?: "Nie udało się utworzyć patelni"
            else -> "Błąd tworzenia patelni: ${e.message}"
        }
    }

    private fun extractServerErrorMessage(raw: String): String? {
        if (raw.isBlank()) return null
        Regex(""""message"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.getOrNull(1)?.let { return it }
        Regex(""""error"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    fun cacheProduct(
        product: ProductDto,
        onError: ((String) -> Unit)? = null,
        onResult: (ProductDto) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val saved = ensureProductSaved(product)
                loadLocalProducts()
                onResult(saved)
            } catch (e: Exception) {
                val message = "Błąd zapisywania produktu: ${e.message}"
                if (onError != null) {
                    onError(message)
                } else {
                    _error.value = message
                }
            }
        }
    }

    fun clearProductSearch() {
        externalSearchJob?.cancel()
        _productSearchQuery.value = ""
        _externalSearchResults.value = emptyList()
        _isSearchingProducts.value = false
    }

    fun searchProductByBarcode(barcode: String) {
        viewModelScope.launch {
            _isSearchingProducts.value = true
            try {
                val product = apiService?.getProductByBarcode(barcode)
                if (product != null) {
                    val saved = ensureProductSaved(product)
                    loadLocalProducts()
                    _scannedProduct.value = saved
                } else {
                    _externalSearchResults.value = emptyList()
                    _barcodeToPrefill.value = barcode
                }
            } catch (e: Exception) {
                _error.value = "Błąd skanowania: ${e.message}"
            } finally {
                _isSearchingProducts.value = false
            }
        }
    }

    fun barcodeHandled() {
        _barcodeToPrefill.value = null
    }

    fun scannedProductHandled() {
        _scannedProduct.value = null
    }

    fun loadLocalProducts() {
        viewModelScope.launch {
            try {
                _localProducts.value = apiService?.getLocalProducts() ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Błąd pobierania produktów z bazy: ${e.message}"
            }
        }
    }

    fun saveLocalProduct(product: ProductDto) {
        viewModelScope.launch {
            try {
                apiService?.saveLocalProduct(product)
                loadLocalProducts()
            } catch (e: Exception) {
                _error.value = "Błąd zapisywania produktu: ${e.message}"
            }
        }
    }

    fun loadRecipes(scope: String = "MINE", singleMeal: Boolean? = null, query: String = "") {
        viewModelScope.launch {
            try {
                val results = apiService?.getRecipes(scope = scope, singleMeal = singleMeal, query = query.takeIf { it.isNotBlank() })
                    ?: emptyList()
                _allRecipes.value = results
                if (query.isNotBlank()) {
                    _recipeSearchResults.value = results
                }
            } catch (e: Exception) {
                _error.value = "Błąd pobierania przepisów: ${e.message}"
            }
        }
    }

    fun searchRecipes(query: String, scope: String = "MINE", singleMeal: Boolean? = null) {
        viewModelScope.launch {
            try {
                val results = if (query.isBlank()) {
                    apiService?.getRecipes(scope = scope, singleMeal = singleMeal) ?: emptyList()
                } else {
                    apiService?.searchRecipes(query, scope = scope, singleMeal = singleMeal) ?: emptyList()
                }
                _recipeSearchResults.value = results
            } catch (e: Exception) {
                _error.value = "Błąd wyszukiwania przepisów: ${e.message}"
            }
        }
    }

    fun saveRecipe(recipe: RecipeDto, onSaved: ((RecipeDto) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val prepared = ensureRecipeProductsCached(recipe)
                val saved = if (prepared.id != null) {
                    apiService?.updateRecipe(prepared.id, prepared)
                } else {
                    apiService?.saveRecipe(prepared)
                }
                if (saved != null) {
                    onSaved?.invoke(saved)
                }
            } catch (e: Exception) {
                _error.value = "Błąd zapisywania przepisu: ${e.message}"
            }
        }
    }

    fun deleteRecipe(id: String, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                if (apiService?.deleteRecipe(id) == true) {
                    onDeleted?.invoke()
                    loadRecipes()
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania przepisu: ${e.message}"
            }
        }
    }

    fun createBatchFromRecipe(recipe: RecipeDto, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                apiService?.createBatchMeal(recipe.toCreateBatchMealRequest())
                refreshActiveBatchMeals()
                onComplete?.invoke()
            } catch (e: Exception) {
                _error.value = "Błąd tworzenia patelni z przepisu: ${e.message}"
            }
        }
    }

    fun createBatchMealAndOptionalRecipe(
        batchRequest: CreateBatchMealRequest,
        onComplete: (() -> Unit)? = null
    ) = createBatchMeal(batchRequest, onComplete)

    fun createBatchMeal(
        request: CreateBatchMealRequest,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                if (request.segments.isEmpty()) {
                    throw IllegalArgumentException("Patelnia musi mieć co najmniej jedną sekcję ze składnikami")
                }
                val prepared = prepareBatchMealRequest(request)
                val service = apiService
                    ?: throw IllegalStateException("Brak połączenia z serwerem")
                service.createBatchMeal(prepared)
                refreshActiveBatchMeals()
                onComplete?.invoke()
            } catch (e: Exception) {
                val message = formatUserFacingError(e)
                if (onError != null) {
                    onError(message)
                } else {
                    _error.value = message
                }
            }
        }
    }

    fun consumeRecipePortions(
        recipe: RecipeDto,
        segmentWeights: Map<Long, Double>,
        mealType: String,
        date: String,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val requests = com.cantbebetter.bowly.ui.screens.buildConsumeRequestsFromRecipe(
                    recipe, segmentWeights, date, mealType
                )
                var allSuccess = requests.isNotEmpty()
                requests.forEach { request ->
                    val success = apiService?.consumeProduct(request) ?: false
                    if (!success) allSuccess = false
                }
                if (allSuccess) {
                    loadDailySummary(date)
                    onComplete?.invoke()
                } else {
                    _error.value = "Nie udało się dodać całego przepisu do posiłku"
                }
            } catch (e: Exception) {
                _error.value = "Błąd dodawania przepisu: ${e.message}"
            }
        }
    }

    private suspend fun refreshActiveBatchMeals() {
        _activeBatchMeals.value = apiService?.getActiveBatchMeals() ?: emptyList()
    }

    private suspend fun ensureRecipeProductsCached(recipe: RecipeDto): RecipeDto {
        val sections = recipe.sections.map { section ->
            section.copy(
                ingredients = section.ingredients.map { ingredient ->
                    ingredient.copy(product = ensureProductSaved(ingredient.product))
                }
            )
        }
        return recipe.copy(sections = sections)
    }

    fun loadActiveBatchMeals() {
        viewModelScope.launch {
            try {
                refreshActiveBatchMeals()
            } catch (e: Exception) {
                _error.value = "Błąd pobierania patelni: ${e.message}"
            }
        }
    }

    fun deleteBatchMeal(id: Long) {
        viewModelScope.launch {
            try {
                val success = apiService?.deleteBatchMeal(id) ?: false
                if (success) {
                    loadActiveBatchMeals()
                } else {
                    _error.value = "Nie udało się usunąć patelni"
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania patelni: ${e.message}"
            }
        }
    }

    fun updateBatchMealCookedWeights(
        meal: BatchMealDto,
        cookedWeights: Map<Long, Double>,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val service = apiService ?: throw IllegalStateException("Brak połączenia z serwerem")
                var latestMeal = meal
                cookedWeights.forEach { (segmentId, cookedWeightG) ->
                    val current = meal.segments.find { it.id == segmentId }?.initialWeightG
                    if (cookedWeightG > 0 && current != cookedWeightG) {
                        latestMeal = service.updateSegmentCookedWeight(
                            meal.id,
                            segmentId,
                            UpdateSegmentCookedWeightRequest(cookedWeightG)
                        )
                    }
                }
                refreshActiveBatchMeals()
                onComplete?.invoke()
            } catch (e: Exception) {
                _error.value = "Błąd aktualizacji wagi sekcji: ${e.message}"
            }
        }
    }

    fun consumePortion(request: ConsumePortionRequest, date: String) {
        viewModelScope.launch {
            try {
                val success = apiService?.consumePortion(request) ?: false
                if (success) {
                    loadActiveBatchMeals()
                    loadDailySummary(date)
                } else {
                    _error.value = "Nie udało się dodać porcji z patelni"
                }
            } catch (e: Exception) {
                _error.value = "Błąd konsumpcji: ${e.message}"
            }
        }
    }

    fun consumeProduct(request: ConsumeProductRequest, date: String) {
        viewModelScope.launch {
            try {
                val success = apiService?.consumeProduct(request) ?: false
                if (success) {
                    loadDailySummary(date)
                }
            } catch (e: Exception) {
                _error.value = "Błąd dodawania produktu: ${e.message}"
            }
        }
    }

    fun consumeRecipe(recipe: RecipeDto, weightG: Double, mealType: String, date: String) {
        viewModelScope.launch {
            try {
                val totalWeightG = recipe.sections.sumOf { it.ingredients.sumOf { ing -> ing.amount } }
                val per100gRatio = if (totalWeightG > 0) 100.0 / totalWeightG else 0.0

                val cals100 = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * per100gRatio / 100.0) * ing.product.calories } }
                val pro100 = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * per100gRatio / 100.0) * ing.product.protein } }
                val fat100 = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * per100gRatio / 100.0) * ing.product.fat } }
                val carb100 = recipe.sections.sumOf { it.ingredients.sumOf { ing -> (ing.amount * per100gRatio / 100.0) * ing.product.carbohydrates } }

                val request = ConsumeProductRequest(
                    product = ProductDto(
                        id = recipe.id,
                        name = recipe.name,
                        calories = cals100,
                        protein = pro100,
                        fat = fat100,
                        carbohydrates = carb100,
                        source = "RECIPE"
                    ),
                    weightG = weightG,
                    mealDate = date,
                    mealType = MealTypeMapper.toApi(mealType)
                )

                val success = apiService?.consumeProduct(request) ?: false
                if (success) {
                    loadDailySummary(date)
                }
            } catch (e: Exception) {
                _error.value = "Błąd dodawania przepisu: ${e.message}"
            }
        }
    }

    fun updateConsumedMeal(id: String, request: ConsumeProductRequest, date: String) {
        viewModelScope.launch {
            try {
                val success = apiService?.updateConsumedMeal(id, request) ?: false
                if (success) {
                    loadDailySummary(date)
                }
            } catch (e: Exception) {
                _error.value = "Błąd aktualizacji: ${e.message}"
            }
        }
    }

    fun deleteConsumedMeal(id: String, date: String) {
        viewModelScope.launch {
            try {
                val success = apiService?.deleteConsumedMeal(id) ?: false
                if (success) {
                    loadActiveBatchMeals()
                    loadDailySummary(date)
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania: ${e.message}"
            }
        }
    }

    fun loadContainers() {
        viewModelScope.launch {
            try {
                _containers.value = apiService?.getContainers() ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Błąd pobierania naczyń: ${e.message}"
            }
        }
    }

    fun createContainer(request: CreateWeighingContainerRequest, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                apiService?.createContainer(request)
                loadContainers()
                onComplete?.invoke()
            } catch (e: Exception) {
                _error.value = "Błąd dodawania naczynia: ${e.message}"
            }
        }
    }

    fun updateContainer(id: Long, request: UpdateWeighingContainerRequest, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                apiService?.updateContainer(id, request)
                loadContainers()
                onComplete?.invoke()
            } catch (e: Exception) {
                _error.value = "Błąd aktualizacji naczynia: ${e.message}"
            }
        }
    }

    fun deleteContainer(id: Long) {
        viewModelScope.launch {
            try {
                val success = apiService?.deleteContainer(id) ?: false
                if (success) {
                    loadContainers()
                } else {
                    _error.value = "Nie udało się usunąć naczynia"
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania naczynia: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun logout() {
        settingsManager.clearSession()
        _userProfile.value = null
        _dailyStats.value = null
        _dailySummary.value = null
        _error.value = null
        val baseUrl = settingsManager.baseUrl
        if (baseUrl.isNullOrBlank()) {
            _uiState.value = AppState.EnterServerAddress
        } else {
            apiService = ApiService(baseUrl, null)
            _uiState.value = AppState.LoginRequired
        }
    }

    private fun mergeSearchResults(
        local: List<ProductDto>,
        external: List<ProductDto>,
        query: String
    ): List<ProductDto> {
        val lowerQuery = query.lowercase()
        val localFiltered = if (lowerQuery.isEmpty()) {
            local
        } else {
            local.filter { it.name.lowercase().contains(lowerQuery) }
        }

        if (query.length < MIN_EXTERNAL_QUERY_LENGTH) {
            return localFiltered
        }

        val localKeys = localFiltered.map { productDedupKey(it) }.toSet()
        val externalFiltered = external
            .filter { it.name.lowercase().contains(lowerQuery) }
            .filter { productDedupKey(it) !in localKeys }

        return localFiltered + externalFiltered
    }

    private fun productDedupKey(product: ProductDto): String {
        if (!product.externalId.isNullOrBlank() && !product.source.isNullOrBlank()) {
            return "${product.source}:${product.externalId}"
        }
        if (!product.externalId.isNullOrBlank()) return "ext:${product.externalId}"
        if (!product.barcode.isNullOrBlank()) return "barcode:${product.barcode}"
        if (product.id != null) return "id:${product.id}"
        return "name:${product.name.lowercase()}"
    }

    companion object {
        private const val EXTERNAL_SEARCH_DEBOUNCE_MS = 800L
        private const val MIN_EXTERNAL_QUERY_LENGTH = 3
    }
}