package com.cantbebetter.bowly.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cantbebetter.bowly.models.*
import com.cantbebetter.bowly.ui.screens.Clock

object MockData {
    var currentUser by mutableStateOf(User(
        id = "1",
        username = "User123",
        email = "user@example.com",
        gender = "MALE",
        age = 28,
        heightCm = 182.0,
        weightKg = 85.0,
        targetWeightKg = 80.0,
        weeklyChangeRateKg = 0.5,
        activityLevel = 1.375,
        macroRatios = MacroRatios(30, 25, 45)
    ))

    val products = mutableStateListOf(
        Product("p1", "Kurczak (Pierś)", 165.0, 31.0, 3.6, 0.0, null, "sztuka", 200.0, MicroElements(), "API"),
        Product("p2", "Ryż Biały", 130.0, 2.7, 0.3, 28.0, "5900000000001", "opakowanie", 100.0, MicroElements(), "API"),
        Product("p3", "Oliwa z oliwek", 884.0, 0.0, 100.0, 0.0, null, "łyżka", 10.0, MicroElements(), "USER"),
        Product("p4", "Sos Pomidorowy", 40.0, 1.5, 0.2, 8.0, "5900000000002", "słoik", 400.0, MicroElements(), "API"),
        Product("p5", "Płatki owsiane", 379.0, 13.0, 7.0, 68.0, null, "opakowanie", 500.0, MicroElements(), "API"),
        Product("p6", "Mleko 2%", 50.0, 3.4, 2.0, 4.7, "5900000000003", "szklanka", 250.0, MicroElements(), "API"),
        Product("p7", "Twaróg chudy", 90.0, 18.0, 0.5, 3.5, null, "kostka", 200.0, MicroElements(), "API"),
        Product("p8", "Ziemniaki gotowane", 77.0, 2.0, 0.1, 17.0, null, "sztuka (średnia)", 90.0, MicroElements(), "API"),
        Product("p9", "Makaron Penne", 150.0, 5.0, 1.0, 30.0, null, "szklanka (gotowany)", 140.0, MicroElements(), "API"),
        Product("p10", "Jajko Kurze", 143.0, 12.6, 9.5, 0.7, null, "sztuka (L)", 56.0, MicroElements(), "API"),
        Product("p11", "Chleb Żytni", 259.0, 8.5, 3.3, 48.0, null, "kromka", 35.0, MicroElements(), "API"),
        Product("p12", "Szynka Konserwowa", 110.0, 18.0, 3.0, 1.5, null, "plaster", 20.0, MicroElements(), "API"),
        Product("p13", "Banan", 89.0, 1.1, 0.3, 23.0, null, "sztuka", 120.0, MicroElements(), "API"),
        Product("p14", "Masło", 717.0, 0.8, 81.0, 0.1, null, "łyżeczka", 5.0, MicroElements(), "API"),
        Product("p15", "Jabłko", 52.0, 0.3, 0.2, 14.0, null, "sztuka", 180.0, MicroElements(), "API")
    )

    val batchMeals = mutableStateListOf(
        BatchMeal(
            "bm1",
            "Obiad na 3 dni (Kurczak z ryżem)",
            listOf(
                BatchMealSegment("s1", "Ryż gotowany", products[1], 600.0, 450.0),
                BatchMealSegment("s2", "Kurczak w sosie", products[0], 500.0, 300.0)
            )
        ),
        BatchMeal(
            "bm2",
            "Zupa krem z pomidorów",
            listOf(
                BatchMealSegment("s3", "Zupa", products[3], 1000.0, 1000.0)
            )
        )
    )

    val consumedMeals = mutableStateListOf(
        ConsumedMeal(
            id = "m1",
            userId = "1",
            name = "Owsianka",
            mealType = "Śniadanie",
            timestamp = 1715413200000L,
            portions = listOf(
                ConsumedPortion("c1", "Płatki owsiane", 50.0, 189.5, 6.5, 3.5, 34.0, "g", 50.0, "p5"),
                ConsumedPortion("c2", "Mleko 2%", 200.0, 100.0, 6.8, 4.0, 9.4, "unit", 0.8, "p6")
            )
        ),
        ConsumedMeal(
            id = "m2",
            userId = "1",
            name = "Obiad na 3 dni (Kurczak z ryżem)",
            mealType = "Obiad",
            timestamp = 1715424000000L,
            isFromBatch = true,
            portions = listOf(
                ConsumedPortion("c3", "Ryż gotowany", 150.0, 195.0, 4.0, 0.4, 42.0, "percent", 25.0, null, "s1"),
                ConsumedPortion("c4", "Kurczak w sosie", 200.0, 330.0, 62.0, 7.2, 0.0, "percent", 40.0, null, "s2")
            )
        ),
        ConsumedMeal(
            id = "m3",
            userId = "1",
            name = "Twaróg chudy",
            mealType = "Kolacja",
            timestamp = 1715452800000L,
            portions = listOf(
                ConsumedPortion("c5", "Twaróg chudy", 150.0, 135.0, 27.0, 0.75, 5.25, "unit", 0.75, "p7")
            )
        )
    )

    var dailyStats by mutableStateOf(calculateDailyStats(currentUser))

    fun calculateDailyStats(user: User): DailyStats {
        // Mifflin-St Jeor Equation
        val bmr = if (user.gender == "MALE") {
            (10 * user.weightKg) + (6.25 * user.heightCm) - (5 * user.age) + 5
        } else {
            (10 * user.weightKg) + (6.25 * user.heightCm) - (5 * user.age) - 161
        }
        
        val tdee = bmr * user.activityLevel
        
        // 1kg of fat is approx 7700 kcal. 
        // Daily offset based on weekly rate: (rate * 7700) / 7
        val calorieOffset = (user.weeklyChangeRateKg * 7700.0) / 7.0
        
        val targetCalories = if (user.targetWeightKg < user.weightKg) {
            tdee - calorieOffset
        } else if (user.targetWeightKg > user.weightKg) {
            tdee + calorieOffset
        } else {
            tdee
        }

        val pRatio = user.macroRatios.protein / 100.0
        val fRatio = user.macroRatios.fat / 100.0
        val cRatio = user.macroRatios.carbs / 100.0

        return DailyStats(
            consumedCalories = 0.0,
            targetCalories = targetCalories,
            protein = 0.0,
            targetProtein = (targetCalories * pRatio) / 4.0,
            fat = 0.0,
            targetFat = (targetCalories * fRatio) / 9.0,
            carbs = 0.0,
            targetCarbs = (targetCalories * cRatio) / 4.0
        )
    }

    fun updateCurrentUser(newUser: User) {
        currentUser = newUser
        dailyStats = calculateDailyStats(newUser)
    }

    fun getMealTypesForDate(timestamp: Long): List<String> {
        val startOfDay = (timestamp / 86400000L) * 86400000L
        return currentUser.dailyMealConfigs[startOfDay] ?: DefaultMealTypes
    }

    fun updateMealTypesFromToday(newTypes: List<String>) {
        val todayStart = (Clock.now() / 86400000L) * 86400000L
        val updatedConfigs = currentUser.dailyMealConfigs.toMutableMap()
        updatedConfigs[todayStart] = newTypes
        // In a real app we'd also store this as the "future default"
        updateCurrentUser(currentUser.copy(dailyMealConfigs = updatedConfigs))
    }

    fun upsertConsumedMeal(meal: ConsumedMeal, oldMeal: ConsumedMeal? = null) {
        // 1. Jeśli to była edycja, wycofujemy stare porcje z patelni
        oldMeal?.let { old ->
            if (old.isFromBatch) {
                undoConsumeFromBatch(old.portions)
            }
        }

        // 2. Dodajemy/aktualizujemy posiłek w liście
        if (oldMeal != null) {
            val index = consumedMeals.indexOfFirst { it.id == oldMeal.id }
            if (index != -1) {
                consumedMeals[index] = meal
            }
        } else {
            consumedMeals.add(0, meal)
        }

        // 3. Jeśli nowy posiłek jest z patelni, odejmujemy wagę
        if (meal.isFromBatch) {
            consumeFromBatch(meal.portions)
        }
    }

    fun deleteConsumedMeal(meal: ConsumedMeal) {
        if (meal.isFromBatch) {
            undoConsumeFromBatch(meal.portions)
        }
        consumedMeals.remove(meal)
    }

    fun consumeFromBatch(portions: List<ConsumedPortion>) {
        portions.forEach { portion ->
            val segmentId = portion.segmentId ?: return@forEach
            batchMeals.forEachIndexed { mealIndex, meal ->
                if (meal.segments.any { it.id == segmentId }) {
                    val updatedSegments = meal.segments.map { s ->
                        if (s.id == segmentId) {
                            s.copy(currentWeightG = (s.currentWeightG - portion.consumedWeightG).coerceAtLeast(0.0))
                        } else s
                    }
                    val isDepleted = updatedSegments.sumOf { it.currentWeightG } < 0.1
                    batchMeals[mealIndex] = meal.copy(segments = updatedSegments, isDepleted = isDepleted)
                }
            }
        }
    }

    private fun undoConsumeFromBatch(portions: List<ConsumedPortion>) {
        portions.forEach { portion ->
            val segmentId = portion.segmentId ?: return@forEach
            batchMeals.forEachIndexed { mealIndex, meal ->
                if (meal.segments.any { it.id == segmentId }) {
                    val updatedSegments = meal.segments.map { s ->
                        if (s.id == segmentId) {
                            // Przywracamy wagę, ale nie więcej niż initialWeight
                            s.copy(currentWeightG = (s.currentWeightG + portion.consumedWeightG).coerceAtMost(s.initialWeightG))
                        } else s
                    }
                    val isDepleted = updatedSegments.sumOf { it.currentWeightG } < 0.1
                    batchMeals[mealIndex] = meal.copy(segments = updatedSegments, isDepleted = isDepleted)
                }
            }
        }
    }
}
