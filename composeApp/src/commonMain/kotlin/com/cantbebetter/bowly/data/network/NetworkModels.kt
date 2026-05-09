package com.cantbebetter.bowly.data.network

import kotlinx.serialization.Serializable

@Serializable
data class SystemStatusResponse(
    val isSetup: Boolean
)

@Serializable
data class SetupRequest(
    val adminUsername: String,
    val adminPassword: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val username: String? = null,
    val role: String? = null,
    val message: String? = null
)

@Serializable
data class AdminKeysDto(
    val spoonacularKey: String,
    val openFoodFactsKey: String
)

@Serializable
data class UserDto(
    val id: Long? = null,
    val username: String,
    val role: String? = null,
    val gender: String = "MALE",
    val age: Int = 25,
    val heightCm: Double = 180.0,
    val weightKg: Double = 80.0,
    val targetWeightKg: Double = 75.0,
    val weeklyChangeRateKg: Double = 0.5,
    val activityLevel: Double = 1.375,
    val proteinRatio: Int = 30,
    val fatRatio: Int = 30,
    val carbsRatio: Int = 40,
    val isDarkTheme: Boolean? = null,
    val showBatchOnboarding: Boolean = true
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

@Serializable
data class ProductDto(
    val id: String? = null,
    val name: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
    val barcode: String? = null,
    val source: String? = null
)

@Serializable
data class RecipeDto(
    val id: String? = null,
    val name: String,
    val sections: List<RecipeSectionDto>
)

@Serializable
data class RecipeSectionDto(
    val id: String? = null,
    val name: String,
    val ingredients: List<RecipeIngredientDto>
)

@Serializable
data class RecipeIngredientDto(
    val product: ProductDto,
    val amount: Double
)

@Serializable
data class BatchMealDto(
    val id: String? = null,
    val name: String,
    val segments: List<BatchMealSegmentDto>,
    val isDepleted: Boolean = false
)

@Serializable
data class BatchMealSegmentDto(
    val id: String? = null,
    val name: String,
    val product: ProductDto,
    val initialWeightG: Double,
    val currentWeightG: Double
)

@Serializable
data class CreateBatchMealRequest(
    val name: String,
    val segments: List<CreateBatchMealSegmentRequest>
)

@Serializable
data class CreateBatchMealSegmentRequest(
    val name: String,
    val productId: String,
    val initialWeightG: Double
)

@Serializable
data class ConsumePortionRequest(
    val segmentId: String,
    val weightG: Double,
    val mealType: String // e.g., "BREAKFAST", "LUNCH"
)

@Serializable
data class ConsumeProductRequest(
    val productId: String,
    val weightG: Double,
    val mealType: String
)

@Serializable
data class DailySummaryDto(
    val totalCalories: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val meals: Map<String, List<ConsumedMealDto>>
)

@Serializable
data class ConsumedMealDto(
    val id: String,
    val segmentName: String,
    val weightG: Double,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val timestamp: Long
)
