package com.cantbebetter.bowly.ui.screens

import com.cantbebetter.bowly.data.network.BatchMealDto
import com.cantbebetter.bowly.data.network.BatchMealSegmentDto
import com.cantbebetter.bowly.data.network.CreateBatchMealRequest
import com.cantbebetter.bowly.data.network.CreateBatchMealSegmentRequest
import com.cantbebetter.bowly.data.network.RecipeDto

fun BatchMealSegmentDto.remainingKcal(): Double {
    if (initialWeightG <= 0) return 0.0
    return totalKcal * (currentWeightG / initialWeightG)
}

fun BatchMealSegmentDto.kcalForWeight(weightG: Double): Double {
    if (initialWeightG <= 0) return 0.0
    return totalKcal * (weightG / initialWeightG)
}

fun BatchMealSegmentDto.remainingProgress(): Float {
    if (initialWeightG <= 0) return 0f
    return (currentWeightG / initialWeightG).toFloat().coerceIn(0f, 1f)
}

fun BatchMealDto.totalCurrentWeightG(): Double = segments.sumOf { it.currentWeightG }

fun BatchMealDto.totalInitialWeightG(): Double = segments.sumOf { it.initialWeightG }

fun BatchMealDto.totalRemainingKcal(): Double = segments.sumOf { it.remainingKcal() }

fun BatchMealDto.overallProgress(): Float {
    val initial = totalInitialWeightG()
    if (initial <= 0) return 0f
    return (totalCurrentWeightG() / initial).toFloat().coerceIn(0f, 1f)
}

fun sectionMacrosFromProducts(products: List<ProductData>): SegmentMacros {
    val totalKcal = products.sumOf { (it.weightG / 100.0) * it.product.calories }
    val totalProtein = products.sumOf { (it.weightG / 100.0) * it.product.protein }
    val totalFat = products.sumOf { (it.weightG / 100.0) * it.product.fat }
    val totalCarbs = products.sumOf { (it.weightG / 100.0) * it.product.carbohydrates }
    return SegmentMacros(totalKcal, totalProtein, totalFat, totalCarbs)
}

fun isLikelyBarcode(query: String): Boolean {
    val trimmed = query.trim()
    return trimmed.length in 8..14 && trimmed.all { it.isDigit() }
}

data class SegmentMacros(
    val totalKcal: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double
)

fun List<SectionData>.toCreateBatchMealSegments(mealName: String): List<CreateBatchMealSegmentRequest> =
    mapNotNull { section ->
        if (section.products.isEmpty()) return@mapNotNull null
        val totalWeight = section.products.sumOf { it.weightG }
        if (totalWeight <= 0) return@mapNotNull null
        val primaryProduct = section.products.first().product
        val macros = sectionMacrosFromProducts(section.products)
        CreateBatchMealSegmentRequest(
            name = section.name.ifBlank { mealName },
            productId = primaryProduct.id,
            product = primaryProduct,
            products = section.products.map { it.product },
            initialWeightG = totalWeight.sanitizeApiDouble(),
            totalKcal = macros.totalKcal.sanitizeApiDouble(),
            totalProtein = macros.totalProtein.sanitizeApiDouble(),
            totalFat = macros.totalFat.sanitizeApiDouble(),
            totalCarbs = macros.totalCarbs.sanitizeApiDouble()
        )
    }

fun buildCreateBatchMealRequest(
    mealName: String,
    sections: List<SectionData>,
    saveAsRecipe: Boolean,
    initialRecipe: RecipeDto? = null,
    initialMeal: BatchMealDto? = null
): CreateBatchMealRequest {
    val shouldSaveRecipe = saveAsRecipe && initialRecipe == null && initialMeal == null
    return CreateBatchMealRequest(
        name = mealName.trim(),
        saveAsRecipe = shouldSaveRecipe,
        recipeSections = if (shouldSaveRecipe) sections.toRecipeSectionsForSave() else emptyList(),
        segments = sections.toCreateBatchMealSegments(mealName)
    )
}

fun validateBatchMealSections(sections: List<SectionData>): String? {
    val hasWeightedProducts = sections.any { section ->
        section.products.isNotEmpty() && section.products.sumOf { it.weightG } > 0
    }
    if (!hasWeightedProducts) {
        return "Dodaj co najmniej jedną sekcję ze składnikami i podaj wagę większą od 0 g"
    }
    return null
}

private fun Double.sanitizeApiDouble(): Double = when {
    isNaN() || isInfinite() -> 0.0
    else -> this
}
