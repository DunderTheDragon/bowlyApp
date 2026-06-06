package com.cantbebetter.bowly.ui.screens

import com.cantbebetter.bowly.data.network.BatchMealDto
import com.cantbebetter.bowly.data.network.BatchMealSegmentDto
import com.cantbebetter.bowly.data.network.ProductDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatchMealHelpersTest {

    private fun segment(
        initial: Double = 1000.0,
        current: Double = 500.0,
        kcal: Double = 1000.0
    ) = BatchMealSegmentDto(
        id = 1,
        name = "Sekcja",
        product = null,
        initialWeightG = initial,
        currentWeightG = current,
        totalKcal = kcal,
        totalProtein = 100.0,
        totalFat = 50.0,
        totalCarbs = 80.0
    )

    @Test
    fun remainingKcal_scalesWithCurrentWeight() {
        assertEquals(500.0, segment().remainingKcal(), 0.01)
    }

    @Test
    fun kcalForWeight_returnsProportionalValue() {
        assertEquals(100.0, segment().kcalForWeight(100.0), 0.01)
    }

    @Test
    fun overallProgress_forBatchMeal() {
        val meal = BatchMealDto(
            id = 1,
            name = "Patelnia",
            recipeId = null,
            isDepleted = false,
            segments = listOf(segment(initial = 1000.0, current = 250.0))
        )
        assertEquals(0.25f, meal.overallProgress())
    }

    @Test
    fun isLikelyBarcode_detectsEAN() {
        assertTrue(isLikelyBarcode("5901234567890"))
        assertFalse(isLikelyBarcode("abc"))
        assertFalse(isLikelyBarcode("123"))
    }

    @Test
    fun validateBatchMealSections_requiresWeightedProducts() {
        val sections = listOf(
            SectionData(id = "s1", name = "A", products = listOf(ProductData(ProductDto(name = "X", calories = 1.0, protein = 0.0, fat = 0.0, carbohydrates = 0.0), 0.0)))
        )
        assertEquals(
            "Dodaj co najmniej jedną sekcję ze składnikami i podaj wagę większą od 0 g",
            validateBatchMealSections(sections)
        )
    }

    @Test
    fun buildCreateBatchMealRequest_setsSaveAsRecipeWhenRequested() {
        val product = ProductDto(id = "1", name = "Ryż", calories = 130.0, protein = 2.0, fat = 0.3, carbohydrates = 28.0)
        val sections = listOf(
            SectionData(id = "s1", name = "Główna", products = listOf(ProductData(product, 200.0)))
        )
        val request = buildCreateBatchMealRequest("Obiad", sections, saveAsRecipe = true)
        assertTrue(request.saveAsRecipe == true)
        assertEquals(1, request.segments.size)
        assertNull(validateBatchMealSections(sections))
    }
}
