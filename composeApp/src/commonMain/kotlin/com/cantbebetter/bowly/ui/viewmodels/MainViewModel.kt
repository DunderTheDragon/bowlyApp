package com.cantbebetter.bowly.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cantbebetter.bowly.data.SettingsManager
import com.cantbebetter.bowly.data.network.*
import com.cantbebetter.bowly.models.DailyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AppState {
    object Loading : AppState()
    object EnterServerAddress : AppState()
    object SetupRequired : AppState()
    object LoginRequired : AppState()
    object Authenticated : AppState()
}

class MainViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    private val _uiState = MutableStateFlow<AppState>(AppState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _adminKeys = MutableStateFlow<AdminKeysDto?>(null)
    val adminKeys = _adminKeys.asStateFlow()

    private val _users = MutableStateFlow<List<UserDto>>(emptyList())
    val users = _users.asStateFlow()

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _dailyStats = MutableStateFlow<DailyStats?>(null)
    val dailyStats = _dailyStats.asStateFlow()

    private val _dailySummary = MutableStateFlow<DailySummaryDto?>(null)
    val dailySummary = _dailySummary.asStateFlow()

    private val _activeBatchMeals = MutableStateFlow<List<BatchMealDto>>(emptyList())
    val activeBatchMeals = _activeBatchMeals.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ProductDto>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _recipeSearchResults = MutableStateFlow<List<RecipeDto>>(emptyList())
    val recipeSearchResults = _recipeSearchResults.asStateFlow()

    private var apiService: ApiService? = null

    init {
        checkStatus()
    }

    fun setServerAddress(url: String) {
        val sanitizedUrl = url.trim().removeSuffix("/")
        settingsManager.baseUrl = sanitizedUrl
        _error.value = null
        checkStatus()
    }

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
                if (status?.isSetup == false) {
                    _uiState.value = AppState.SetupRequired
                } else {
                    if (settingsManager.token.isNullOrBlank()) {
                        _uiState.value = AppState.LoginRequired
                    } else {
                        _uiState.value = AppState.Authenticated
                    }
                }
            } catch (e: Exception) {
                _error.value = "Nie można połączyć się z serwerem: ${e.message}"
                _uiState.value = AppState.EnterServerAddress
            }
        }
    }

    fun setupAdmin(request: SetupRequest) {
        viewModelScope.launch {
            try {
                val success = apiService?.setupAdmin(request) ?: false
                if (success) {
                    _uiState.value = AppState.LoginRequired
                }
            } catch (e: Exception) {
                _error.value = "Błąd konfiguracji: ${e.message}"
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
                    _users.value = apiService?.getUsers() ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Błąd rejestracji użytkownika: ${e.message}"
            }
        }
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
        viewModelScope.launch {
            try {
                val success = apiService?.updateUserProfile(user) ?: false
                if (success) {
                    _userProfile.value = user
                    _dailyStats.value = calculateDailyStats(user)
                    _error.value = "Profil zaktualizowany"
                }
            } catch (e: Exception) {
                _error.value = "Błąd aktualizacji profilu: ${e.message}"
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

    // Products & Recipes
    fun searchProducts(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _searchResults.value = apiService?.searchProducts(query) ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Błąd wyszukiwania: ${e.message}"
            }
        }
    }

    fun addLocalProduct(product: ProductDto) {
        viewModelScope.launch {
            try {
                apiService?.addLocalProduct(product)
                _error.value = "Produkt dodany pomyślnie"
            } catch (e: Exception) {
                _error.value = "Błąd dodawania produktu: ${e.message}"
            }
        }
    }

    fun searchRecipes(query: String) {
        if (query.isBlank()) {
            _recipeSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _recipeSearchResults.value = apiService?.searchRecipes(query) ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Błąd wyszukiwania przepisów: ${e.message}"
            }
        }
    }

    fun saveRecipe(recipe: RecipeDto) {
        viewModelScope.launch {
            try {
                apiService?.saveRecipe(recipe)
                _error.value = "Przepis zapisany pomyślnie"
            } catch (e: Exception) {
                _error.value = "Błąd zapisywania przepisu: ${e.message}"
            }
        }
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            try {
                if (apiService?.deleteRecipe(id) == true) {
                    _error.value = "Przepis usunięty"
                    // Optionally refresh list if we had one
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania przepisu: ${e.message}"
            }
        }
    }

    // Batch Meals
    fun loadActiveBatchMeals() {
        viewModelScope.launch {
            try {
                _activeBatchMeals.value = apiService?.getActiveBatchMeals() ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Błąd pobierania patelni: ${e.message}"
            }
        }
    }

    fun createBatchMeal(request: CreateBatchMealRequest) {
        viewModelScope.launch {
            try {
                apiService?.createBatchMeal(request)
                loadActiveBatchMeals()
                _error.value = "Patelnia utworzona pomyślnie"
            } catch (e: Exception) {
                _error.value = "Błąd tworzenia patelni: ${e.message}"
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
                    _error.value = "Porcja zjedzona!"
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
                    _error.value = "Produkt dodany!"
                }
            } catch (e: Exception) {
                _error.value = "Błąd dodawania produktu: ${e.message}"
            }
        }
    }

    fun updateConsumedMeal(id: String, request: ConsumeProductRequest, date: String) {
        viewModelScope.launch {
            try {
                val success = apiService?.updateConsumedMeal(id, request) ?: false
                if (success) {
                    loadDailySummary(date)
                    _error.value = "Produkt zaktualizowany!"
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
                    _error.value = "Usunięto posiłek"
                }
            } catch (e: Exception) {
                _error.value = "Błąd usuwania: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun logout() {
        settingsManager.token = null
        settingsManager.username = null
        settingsManager.role = null
        _uiState.value = AppState.LoginRequired
    }
}
