package com.cantbebetter.bowly.ui.screens

import com.cantbebetter.bowly.data.network.BatchMealDto
import com.cantbebetter.bowly.data.network.BatchMealSegmentDto

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
