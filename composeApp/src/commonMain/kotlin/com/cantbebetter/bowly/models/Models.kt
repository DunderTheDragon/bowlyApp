package com.cantbebetter.bowly.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    val gender: String = "MALE", // "MALE", "FEMALE"
    val age: Int = 25,
    val heightCm: Double = 180.0,
    val weightKg: Double = 80.0,
    val targetWeightKg: Double = 75.0,
    val weeklyChangeRateKg: Double = 0.5,
    val activityLevel: Double = 1.375, // Sedentary: 1.2, Light: 1.375, Moderate: 1.55, Active: 1.725
    val macroRatios: MacroRatios = MacroRatios(30, 30, 40), // P, F, C in %
    val isDarkTheme: Boolean? = null, // null means system default
    val showBatchOnboarding: Boolean = true,
    val dailyMealConfigs: Map<Long, List<String>> = emptyMap() // Start of day (millis) -> List of meal names
)

val DefaultMealTypes = listOf("Śniadanie", "Obiad", "Kolacja")
val AllAvailableMealTypes = listOf("Śniadanie", "II Śniadanie", "Obiad", "Podwieczorek", "Kolacja")

@Serializable
data class MacroRatios(
    val protein: Int,
    val fat: Int,
    val carbs: Int
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val calories: Double, // per 100g
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val barcode: String? = null,
    val unitName: String = "opakowanie",
    val unitWeightG: Double = 100.0,
    val micros: MicroElements = MicroElements(),
    val source: String // "API" or "USER"
)

@Serializable
data class MicroElements(
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null,
    val saturatedFat: Double? = null
)

@Serializable
data class BatchMeal(
    val id: String,
    val name: String,
    val segments: List<BatchMealSegment>,
    val isDepleted: Boolean = false
)

@Serializable
data class BatchMealSegment(
    val id: String,
    val name: String,
    val product: Product,
    val initialWeightG: Double,
    val currentWeightG: Double
) {
    val calories: Double get() = (currentWeightG / 100.0) * product.calories
    val protein: Double get() = (currentWeightG / 100.0) * product.protein
    val fat: Double get() = (currentWeightG / 100.0) * product.fat
    val carbs: Double get() = (currentWeightG / 100.0) * product.carbs
}

@Serializable
data class ConsumedPortion(
    val id: String,
    val segmentName: String,
    val consumedWeightG: Double,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    // Przechowujemy oryginalne parametry dla powrotu do edycji
    val originalUnitType: String = "g", // "g", "unit", "percent"
    val originalValue: Double = 0.0,
    val productId: String? = null,
    val segmentId: String? = null
)

@Serializable
data class ConsumedMeal(
    val id: String,
    val userId: String,
    val name: String, // Nazwa wyświetlana (np. "Kurczak z ryżem" lub nazwa produktu)
    val mealType: String, // Śniadanie, Obiad...
    val portions: List<ConsumedPortion>,
    val timestamp: Long,
    val isFromBatch: Boolean = false
) {
    val totalCalories get() = portions.sumOf { it.calories }
    val totalProtein get() = portions.sumOf { it.protein }
    val totalFat get() = portions.sumOf { it.fat }
    val totalCarbs get() = portions.sumOf { it.carbs }
}

@Serializable
data class DailyStats(
    val consumedCalories: Double,
    val targetCalories: Double,
    val protein: Double,
    val targetProtein: Double,
    val fat: Double,
    val targetFat: Double,
    val carbs: Double,
    val targetCarbs: Double
)
