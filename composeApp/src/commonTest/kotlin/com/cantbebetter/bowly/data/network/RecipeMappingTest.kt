package com.cantbebetter.bowly.data.network

import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeMappingTest {

    @Test
    fun apiMealRecipeDto_mapsToRecipeDto() {
        val api = ApiMealRecipeDto(
            id = 5,
            name = "Obiad",
            sections = listOf(
                ApiRecipeSectionDto(
                    name = "Główna",
                    ingredients = listOf(
                        ApiRecipeIngredientDto(
                            productId = 1,
                            productName = "Kurczak",
                            weightG = 150.0,
                            calories = 165.0,
                            protein = 31.0,
                            fat = 3.6,
                            carbohydrates = 0.0
                        )
                    )
                )
            )
        )

        val recipe = api.toRecipeDto()

        assertEquals("5", recipe.id)
        assertEquals("Obiad", recipe.name)
        assertEquals("Kurczak", recipe.sections.first().ingredients.first().product.name)
        assertEquals(150.0, recipe.sections.first().ingredients.first().amount)
    }

    @Test
    fun recipeDto_roundTripsToCreateRequest() {
        val recipe = RecipeDto(
            id = "10",
            name = "Test",
            sections = listOf(
                RecipeSectionDto(
                    name = "Sekcja",
                    ingredients = listOf(
                        RecipeIngredientDto(
                            product = ProductDto(id = "2", name = "Tofu", calories = 144.0, protein = 15.0, fat = 8.0, carbohydrates = 3.0),
                            amount = 100.0
                        )
                    )
                )
            )
        )

        val request = recipe.toCreateRequest()

        assertEquals("Test", request.name)
        assertEquals(2L, request.sections.first().ingredients.first().productId)
        assertEquals(100.0, request.sections.first().ingredients.first().weightG)
    }
}
