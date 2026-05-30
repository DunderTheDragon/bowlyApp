package com.cantbebetter.bowly.data.network

import kotlinx.serialization.Serializable

@Serializable
data class SystemStatusResponse(
    val isSetup: Boolean
)

@Serializable
data class SetupRequest(
    val adminUsername: String,
    val adminPassword: String,
    val spoonacularApiKey: String? = null,
    val openFoodFactsKey: String? = null
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
    val proteinRatio: Double = 30.0,
    val fatRatio: Double = 30.0,
    val carbsRatio: Double = 40.0,
    val isDarkTheme: Boolean? = null,
    val showBatchOnboarding: Boolean = true
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val registrationSecret: String
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
    val source: String? = null,
    val externalId: String? = null,
    val unitName: String? = null,
    val unitWeightG: Double? = null
)

@Serializable
data class RecipeDto(
    val id: String? = null,
    val name: String,
    val sections: List<RecipeSectionDto> = emptyList(),
    val isSingleMeal: Boolean = false,
    val userId: String? = null,
    val username: String? = null
)

@Serializable
data class RecipeSectionDto(
    val id: String? = null,
    val name: String,
    val ingredients: List<RecipeIngredientDto> = emptyList()
)

@Serializable
data class RecipeIngredientDto(
    val product: ProductDto,
    val amount: Double
)

@Serializable
data class BatchMealDto(
    val id: Long,
    val name: String,
    val recipeId: Long?,
    val isDepleted: Boolean,
    val segments: List<BatchMealSegmentDto>
)

@Serializable
data class BatchMealSegmentDto(
    val id: Long,
    val name: String,
    val product: ProductDto?,
    val initialWeightG: Double,
    val currentWeightG: Double,
    val totalKcal: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double
)

@Serializable
data class CreateBatchMealRequest(
    val name: String,
    val recipeId: Long? = null,
    val saveAsRecipe: Boolean = false,
    val recipeSections: List<CreateRecipeSectionApiRequest>? = null,
    val segments: List<CreateBatchMealSegmentRequest>
)

@Serializable
data class CreateBatchMealSegmentRequest(
    val name: String,
    val productId: String? = null,
    val product: ProductDto? = null,
    val products: List<ProductDto>? = null,
    val initialWeightG: Double,
    val totalKcal: Double? = null,
    val totalProtein: Double? = null,
    val totalFat: Double? = null,
    val totalCarbs: Double? = null
)

@Serializable
data class ConsumePortionRequest(
    val segmentId: Long,
    val weightG: Double,
    val mealDate: String,
    val mealType: String // e.g., "BREAKFAST", "LUNCH"
)

@Serializable
data class ConsumeProductRequest(
    val product: ProductDto,
    val weightG: Double,
    val mealDate: String,
    val mealType: String
)

@Serializable
data class DailySummaryDto(
    val date: String,
    val totalKcal: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val burnedKcal: Double = 0.0,
    val workouts: List<WorkoutActivityDto> = emptyList(),
    val meals: Map<String, MealSummaryDto>
)

@Serializable
data class WorkoutActivityDto(
    val id: Long,
    val name: String,
    val caloriesBurned: Double,
    val activityDate: String
)

@Serializable
data class CreateWorkoutActivityRequest(
    val name: String,
    val caloriesBurned: Double,
    val activityDate: String
)

@Serializable
data class MealSummaryDto(
    val mealType: String,
    val totalKcal: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val portions: List<ConsumedPortionDto>
)

@Serializable
data class ConsumedPortionDto(
    val id: Long,
    val segmentName: String?,
    val batchMealName: String?,
    val productName: String?,
    val consumedWeightG: Double,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)