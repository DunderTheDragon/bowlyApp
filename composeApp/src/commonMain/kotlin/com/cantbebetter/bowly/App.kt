package com.cantbebetter.bowly

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.cantbebetter.bowly.ui.screens.BatchMealsScreen
import com.cantbebetter.bowly.ui.screens.DashboardScreen
import com.cantbebetter.bowly.ui.screens.ProfileScreen
import com.cantbebetter.bowly.ui.screens.AddMealSelectionScreen
import androidx.compose.ui.tooling.preview.Preview
import com.cantbebetter.bowly.data.MockData
import com.cantbebetter.bowly.models.ConsumedMeal

enum class Screen {
    Dashboard, BatchMeals, Profile, AddMealSelection
}

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var activeMealName by remember { mutableStateOf("Inne") }
    var editingMeal by remember { mutableStateOf<ConsumedMeal?>(null) }
    
    val user = MockData.currentUser
    val darkTheme = when (user.isDarkTheme) {
        true -> true
        false -> false
        null -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            secondary = androidx.compose.ui.graphics.Color(0xFF8BC34A)
        )
    ) {
        Scaffold(
            bottomBar = {
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
        ) { innerPadding ->
            Surface(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    Screen.Dashboard -> DashboardScreen(
                        onAddMealClick = { mealName ->
                            activeMealName = mealName
                            editingMeal = null
                            currentScreen = Screen.AddMealSelection
                        },
                        onEditMealClick = { meal ->
                            editingMeal = meal
                            activeMealName = meal.mealType
                            currentScreen = Screen.AddMealSelection
                        },
                        onDeleteMealClick = { meal ->
                            MockData.consumedMeals.remove(meal)
                        }
                    )

                    Screen.BatchMeals -> BatchMealsScreen()
                    Screen.AddMealSelection -> AddMealSelectionScreen(
                        mealName = activeMealName,
                        initialMeal = editingMeal,
                        onBack = { currentScreen = Screen.Dashboard },
                        onConfirm = { meal ->
                            if (editingMeal != null) {
                                val index = MockData.consumedMeals.indexOfFirst { it.id == editingMeal!!.id }
                                if (index != -1) {
                                    MockData.consumedMeals[index] = meal
                                }
                            } else {
                                MockData.consumedMeals.add(0, meal)
                                if (meal.isFromBatch) {
                                    MockData.consumeFromBatch(meal.portions)
                                }
                            }
                            currentScreen = Screen.Dashboard
                        }
                    )

                    Screen.Profile -> ProfileScreen()
                }
            }
        }
    }
}
