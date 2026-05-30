package com.cantbebetter.bowly.data.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiMealRecipeDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val tags: String? = null,
    val source: String = "LOCAL",
    val userId: Long? = null,
    val username: String? = null,
    val isSingleMeal: Boolean = false,
    val sections: List<ApiRecipeSectionDto> = emptyList()
)

@Serializable
data class ApiRecipeSectionDto(
    val name: String,
    val ingredients: List<ApiRecipeIngredientDto> = emptyList()
)

@Serializable
data class ApiRecipeIngredientDto(
    val productId: Long,
    val productName: String,
    val weightG: Double,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val barcode: String? = null,
    val source: String? = null,
    val externalId: String? = null
)

@Serializable
data class CreateMealRecipeApiRequest(
    val name: String,
    val description: String? = null,
    val tags: String? = null,
    val isSingleMeal: Boolean = false,
    val sections: List<CreateRecipeSectionApiRequest>
)

@Serializable
data class CreateRecipeSectionApiRequest(
    val name: String,
    val ingredients: List<CreateRecipeIngredientApiRequest>
)

@Serializable
data class CreateRecipeIngredientApiRequest(
    val productId: Long? = null,
    val product: ProductDto? = null,
    val weightG: Double
)

fun ApiMealRecipeDto.toRecipeDto(): RecipeDto = RecipeDto(
    id = id.toString(),
    name = name,
    isSingleMeal = isSingleMeal,
    userId = userId?.toString(),
    username = username,
    sections = sections.map { section ->
        RecipeSectionDto(
            name = section.name,
            ingredients = section.ingredients.map { ingredient ->
                RecipeIngredientDto(
                    product = ProductDto(
                        id = ingredient.productId.toString(),
                        name = ingredient.productName,
                        calories = ingredient.calories,
                        protein = ingredient.protein,
                        fat = ingredient.fat,
                        carbohydrates = ingredient.carbohydrates,
                        barcode = ingredient.barcode,
                        source = ingredient.source,
                        externalId = ingredient.externalId
                    ),
                    amount = ingredient.weightG
                )
            }
        )
    }
)

fun RecipeDto.toCreateRequest(): CreateMealRecipeApiRequest = CreateMealRecipeApiRequest(
    name = name,
    isSingleMeal = isSingleMeal,
    sections = sections.map { section ->
        CreateRecipeSectionApiRequest(
            name = section.name,
            ingredients = section.ingredients.map { ingredient ->
                CreateRecipeIngredientApiRequest(
                    productId = ingredient.product.id?.toLongOrNull(),
                    product = if (ingredient.product.id == null) ingredient.product else null,
                    weightG = ingredient.amount
                )
            }
        )
    }
)
