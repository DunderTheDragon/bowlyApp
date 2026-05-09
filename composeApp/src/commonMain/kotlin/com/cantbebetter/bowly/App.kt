package com.cantbebetter.bowly

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cantbebetter.bowly.ui.screens.BatchMealsScreen
import com.cantbebetter.bowly.ui.screens.DashboardScreen
import com.cantbebetter.bowly.ui.screens.ProfileScreen
import com.cantbebetter.bowly.ui.screens.AddMealSelectionScreen
import com.cantbebetter.bowly.ui.screens.MyProductsScreen
import com.cantbebetter.bowly.ui.screens.Clock
import com.cantbebetter.bowly.data.SettingsManager
import com.cantbebetter.bowly.data.network.ConsumedMealDto
import com.cantbebetter.bowly.ui.admin.AdminScreen
import com.cantbebetter.bowly.ui.auth.LoginScreen
import com.cantbebetter.bowly.ui.auth.ServerAddressScreen
import com.cantbebetter.bowly.ui.auth.SetupAdminScreen
import com.cantbebetter.bowly.ui.viewmodels.AppState
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

enum class Screen {
    Dashboard, BatchMeals, Profile, AddMealSelection, MyProducts, Admin
}

@Composable
fun App() {
    val settingsManager = remember { SettingsManager() }
    val viewModel = remember { MainViewModel(settingsManager) }
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            secondary = androidx.compose.ui.graphics.Color(0xFF8BC34A)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is AppState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AppState.EnterServerAddress -> {
                    ServerAddressScreen(
                        error = error,
                        onSetAddress = { viewModel.setServerAddress(it) }
                    )
                }
                is AppState.SetupRequired -> {
                    SetupAdminScreen(
                        error = error,
                        onSetup = { viewModel.setupAdmin(it) }
                    )
                }
                is AppState.LoginRequired -> {
                    LoginScreen(
                        error = error,
                        onLogin = { viewModel.login(it) }
                    )
                }
                is AppState.Authenticated -> {
                    MainAppContent(viewModel, settingsManager)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel, settingsManager: SettingsManager) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var activeMealName by remember { mutableStateOf("Inne") }
    var editingMeal by remember { mutableStateOf<ConsumedMealDto?>(null) }

    val userProfile by viewModel.userProfile.collectAsState()
    val loggedInUsername = remember { settingsManager.username ?: userProfile?.username ?: "Użytkownik" }
    val userRole = remember { settingsManager.role }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.Admin) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Dzień") },
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Patelnie") },
                        label = { Text("Patelnie") },
                        selected = currentScreen == Screen.BatchMeals,
                        onClick = { currentScreen = Screen.BatchMeals }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profil") },
                        label = { Text("Profil") },
                        selected = currentScreen == Screen.Profile,
                        onClick = { currentScreen = Screen.Profile }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    viewModel = viewModel,
                    onAddMealClick = { mealName ->
                        activeMealName = mealName
                        editingMeal = null
                        currentScreen = Screen.AddMealSelection
                    },
                    onEditMealClick = { meal ->
                        editingMeal = meal
                        activeMealName = "Edytuj" // Mapowanie typu posiłku z DTO jeśli potrzeba
                        currentScreen = Screen.AddMealSelection
                    },
                    onDeleteMealClick = { meal ->
                        viewModel.deleteConsumedMeal(meal.id, Clock.formatToApiDate(Clock.now())) // Simplified date
                    }
                )

                Screen.BatchMeals -> BatchMealsScreen(viewModel = viewModel)
                Screen.AddMealSelection -> AddMealSelectionScreen(
                    viewModel = viewModel,
                    mealName = activeMealName,
                    initialMeal = editingMeal,
                    onBack = { currentScreen = Screen.Dashboard },
                    onConfirm = { currentScreen = Screen.Dashboard }
                )

                Screen.Profile -> ProfileScreen(
                    viewModel = viewModel,
                    onMyProductsClick = { currentScreen = Screen.MyProducts },
                    onAdminPanelClick = { currentScreen = Screen.Admin },
                    onLogoutClick = { viewModel.logout() }
                )
                Screen.MyProducts -> MyProductsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Profile }
                )
                Screen.Admin -> AdminScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Profile }
                )
            }
        }
    }
}
