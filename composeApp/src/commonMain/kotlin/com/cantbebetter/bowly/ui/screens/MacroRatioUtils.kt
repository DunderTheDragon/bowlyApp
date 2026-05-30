package com.cantbebetter.bowly.ui.screens

import com.cantbebetter.bowly.data.network.UserDto
import kotlin.math.abs
import kotlin.math.floor

data class MacroRatioValues(
    val protein: Double,
    val fat: Double,
    val carbs: Double
) {
    fun normalized(): MacroRatioValues {
        val sum = protein + fat + carbs
        if (sum <= 0.0) {
            return MacroRatioValues(100.0 / 3.0, 100.0 / 3.0, 100.0 / 3.0)
        }
        return MacroRatioValues(
            protein = protein / sum * 100.0,
            fat = fat / sum * 100.0,
            carbs = carbs / sum * 100.0
        )
    }
}

data class MacroRatioDisplay(
    val protein: Int,
    val fat: Int,
    val carbs: Int
) {
    val sum: Int get() = protein + fat + carbs
}

enum class MacroField {
    PROTEIN, FAT, CARBS
}

fun UserDto.macroValues(): MacroRatioValues =
    MacroRatioValues(proteinRatio, fatRatio, carbsRatio).normalized()

fun MacroRatioValues.toDisplay(): MacroRatioDisplay {
    val raw = listOf(
        "protein" to protein,
        "fat" to fat,
        "carbs" to carbs
    )
    val floored = raw.associate { (key, value) -> key to floor(value).toInt() }.toMutableMap()
    var remaining = 100 - floored.values.sum()

    val byFraction = raw
        .map { (key, value) -> key to (value - floor(value)) }
        .sortedByDescending { it.second }

    var index = 0
    while (remaining > 0) {
        val key = byFraction[index % byFraction.size].first
        floored[key] = floored.getValue(key) + 1
        remaining--
        index++
    }

    return MacroRatioDisplay(
        protein = floored.getValue("protein"),
        fat = floored.getValue("fat"),
        carbs = floored.getValue("carbs")
    )
}

fun adjustMacroRatios(
    current: MacroRatioValues,
    changed: MacroField,
    newValueForChanged: Int
): MacroRatioValues {
    val target = newValueForChanged.coerceIn(0, 100).toDouble()
    val remaining = 100.0 - target

    val adjusted = when (changed) {
        MacroField.PROTEIN -> {
            val sumOthers = current.fat + current.carbs
            if (sumOthers <= 0.0) {
                MacroRatioValues(target, remaining / 2.0, remaining / 2.0)
            } else {
                MacroRatioValues(
                    protein = target,
                    fat = current.fat / sumOthers * remaining,
                    carbs = current.carbs / sumOthers * remaining
                )
            }
        }
        MacroField.FAT -> {
            val sumOthers = current.protein + current.carbs
            if (sumOthers <= 0.0) {
                MacroRatioValues(remaining / 2.0, target, remaining / 2.0)
            } else {
                MacroRatioValues(
                    protein = current.protein / sumOthers * remaining,
                    fat = target,
                    carbs = current.carbs / sumOthers * remaining
                )
            }
        }
        MacroField.CARBS -> {
            val sumOthers = current.protein + current.fat
            if (sumOthers <= 0.0) {
                MacroRatioValues(remaining / 2.0, remaining / 2.0, target)
            } else {
                MacroRatioValues(
                    protein = current.protein / sumOthers * remaining,
                    fat = current.fat / sumOthers * remaining,
                    carbs = target
                )
            }
        }
    }

    val sum = adjusted.protein + adjusted.fat + adjusted.carbs
    return if (abs(sum - 100.0) < 0.001) adjusted else adjusted.normalized()
}

fun MacroRatioValues.applyTo(user: UserDto): UserDto = user.copy(
    proteinRatio = protein,
    fatRatio = fat,
    carbsRatio = carbs
)
