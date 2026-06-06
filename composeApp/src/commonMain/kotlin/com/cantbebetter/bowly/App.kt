package com.cantbebetter.bowly

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.cantbebetter.bowly.ui.screens.*
import com.cantbebetter.bowly.data.SettingsManager
import com.cantbebetter.bowly.data.network.ConsumedPortionDto
import com.cantbebetter.bowly.ui.admin.AdminScreen
import com.cantbebetter.bowly.ui.auth.LoginScreen
import com.cantbebetter.bowly.ui.auth.ServerAddressScreen
import com.cantbebetter.bowly.ui.viewmodels.AppState
import com.cantbebetter.bowly.ui.viewmodels.MainViewModel

enum class Screen {
    Dashboard, BatchMeals, Profile, AddMealSelection, MyProducts, MyContainers, Admin
}

@Composable
fun App() {
    val settingsManager = remember { SettingsManager() }
    val viewModel = remember { MainViewModel(settingsManager) }
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val darkTheme = when (userProfile?.isDarkTheme) {
        true -> true
        false -> false
        null -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            secondary = androidx.compose.ui.graphics.Color(0xFF8BC34A)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is AppState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AppState.EnterServerAddress -> {
                    val currentAddress = viewModel.currentServerAddress
                    ServerAddressScreen(
                        error = error,
                        initialAddress = currentAddress,
                        onSetAddress = { viewModel.setServerAddress(it) },
                        onCancel = if (!currentAddress.isNullOrBlank()) {
                            { viewModel.cancelChangeServerAddress() }
                        } else {
                            null
                        }
                    )
                }
                is AppState.LoginRequired -> {
                    LoginScreen(
                        viewModel = viewModel,
                        error = error,
                        serverAddress = viewModel.currentServerAddress,
                        onLogin = { viewModel.login(it) },
                        onRegister = { viewModel.registerUser(it) },
                        onChangeServer = { viewModel.openChangeServerAddress() }
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
    var activeDate by remember { mutableStateOf(Clock.todayApiDate()) }
    var editingMeal by remember { mutableStateOf<ConsumedPortionDto?>(null) }
    var dayOffset by remember { mutableStateOf(0) }
    var lastKnownToday by remember { mutableStateOf(Clock.todayApiDate()) }
    var hasHandledResume by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
        viewModel.loadDailySummary(Clock.todayApiDate())
    }

    LifecycleResumeEffect(Unit) {
        if (hasHandledResume) {
            val today = Clock.todayApiDate()
            if (today != lastKnownToday) {
                lastKnownToday = today
                dayOffset = 0
                activeDate = today
                viewModel.loadDailySummary(today)
            }
        } else {
            hasHandledResume = true
        }
        onPauseOrDispose { }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.Admin && currentScreen != Screen.MyProducts && currentScreen != Screen.MyContainers && currentScreen != Screen.AddMealSelection) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Dzień") },
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Patelnie") },
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
        Surface(modifier = Modifier.padding(if (currentScreen == Screen.Admin || currentScreen == Screen.MyProducts || currentScreen == Screen.MyContainers || currentScreen == Screen.AddMealSelection) PaddingValues(0.dp) else innerPadding)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    viewModel = viewModel,
                    dayOffset = dayOffset,
                    onDayOffsetChange = { dayOffset = it },
                    onAddMealClick = { mealName, date ->
                        activeMealName = mealName
                        activeDate = date
                        editingMeal = null
                        currentScreen = Screen.AddMealSelection
                    },
                    onEditMealClick = { meal ->
                        editingMeal = meal
                        activeMealName = "Inne"
                        currentScreen = Screen.AddMealSelection
                    }
                )

                Screen.BatchMeals -> BatchMealsScreen(viewModel = viewModel)
                Screen.AddMealSelection -> AddMealSelectionScreen(
                    viewModel = viewModel,
                    mealName = activeMealName,
                    date = activeDate,
                    initialMeal = editingMeal,
                    onBack = { currentScreen = Screen.Dashboard },
                    onConfirm = { currentScreen = Screen.Dashboard }
                )

                Screen.Profile -> ProfileScreen(
                    viewModel = viewModel,
                    onMyProductsClick = { currentScreen = Screen.MyProducts },
                    onMyContainersClick = { currentScreen = Screen.MyContainers },
                    onAdminPanelClick = { currentScreen = Screen.Admin },
                    onLogoutClick = { viewModel.logout() }
                )
                Screen.MyProducts -> MyProductsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Profile }
                )
                Screen.MyContainers -> MyContainersScreen(
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