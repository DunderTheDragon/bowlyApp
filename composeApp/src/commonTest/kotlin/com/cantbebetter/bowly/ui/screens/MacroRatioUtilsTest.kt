package com.cantbebetter.bowly.ui.screens

import com.cantbebetter.bowly.data.network.UserDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacroRatioUtilsTest {

    @Test
    fun normalized_dividesProportionsTo100() {
        val values = MacroRatioValues(30.0, 30.0, 40.0).normalized()
        assertEquals(100.0, values.protein + values.fat + values.carbs, 0.001)
    }

    @Test
    fun normalized_handlesZeroSum() {
        val values = MacroRatioValues(0.0, 0.0, 0.0).normalized()
        assertEquals(100.0, values.protein + values.fat + values.carbs, 0.001)
    }

    @Test
    fun toDisplay_sumsTo100() {
        val display = MacroRatioValues(33.3, 33.3, 33.4).toDisplay()
        assertEquals(100, display.sum)
    }

    @Test
    fun adjustMacroRatios_keepsSum100WhenChangingProtein() {
        val adjusted = adjustMacroRatios(
            current = MacroRatioValues(30.0, 30.0, 40.0),
            changed = MacroField.PROTEIN,
            newValueForChanged = 40
        )
        assertEquals(40.0, adjusted.protein, 0.01)
        assertEquals(100.0, adjusted.protein + adjusted.fat + adjusted.carbs, 0.01)
    }

    @Test
    fun userDto_macroValuesUsesRatios() {
        val user = UserDto(
            id = 1,
            username = "u",
            proteinRatio = 25.0,
            fatRatio = 25.0,
            carbsRatio = 50.0
        )
        val values = user.macroValues()
        assertEquals(25.0, values.protein, 0.01)
        assertEquals(50.0, values.carbs, 0.01)
    }
}
