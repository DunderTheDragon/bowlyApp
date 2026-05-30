package com.cantbebetter.bowly.ui.screens

import com.cantbebetter.bowly.data.network.*
import com.cantbebetter.bowly.models.MealTypeMapper

fun RecipeDto.toSectionDataList(): List<SectionData> =
    sections.map { section ->
        SectionData(
            id = "s_${section.name.hashCode()}",
            name = section.name,
            products = section.ingredients.map { ingredient ->
                ProductData(product = ingredient.product, weightG = ingredient.amount)
            }
        )
    }

fun List<SectionData>.toRecipeDto(name: String): RecipeDto = RecipeDto(
    name = name,
    sections = mapNotNull { section ->
        val ingredients = section.products
            .filter { it.weightG > 0 }
            .map { productData ->
                RecipeIngredientDto(
                    product = productData.product,
                    amount = productData.weightG
                )
            }
        if (ingredients.isEmpty()) return@mapNotNull null
        RecipeSectionDto(
            name = section.name,
            ingredients = ingredients
        )
    }
)

fun List<SectionData>.toRecipeSectionsForSave(): List<CreateRecipeSectionApiRequest> =
    mapNotNull { section ->
        val ingredients = section.products
            .filter { it.weightG > 0 }
            .map { productData ->
                CreateRecipeIngredientApiRequest(
                    productId = productData.product.id?.toLongOrNull(),
                    product = productData.product,
                    weightG = productData.weightG
                )
            }
        if (ingredients.isEmpty()) return@mapNotNull null
        CreateRecipeSectionApiRequest(
            name = section.name.ifBlank { "Główna część" },
            ingredients = ingredients
        )
    }

fun RecipeDto.toCreateBatchMealRequest(): CreateBatchMealRequest = CreateBatchMealRequest(
    name = name,
    recipeId = id?.toLongOrNull(),
    segments = sections.mapNotNull { section ->
        if (section.ingredients.isEmpty()) return@mapNotNull null
        val products = section.ingredients.map { ProductData(it.product, it.amount) }
        val totalWeight = products.sumOf { it.weightG }
        if (totalWeight <= 0) return@mapNotNull null
        val primaryProduct = products.first().product
        val macros = sectionMacrosFromProducts(products)
        CreateBatchMealSegmentRequest(
            name = section.name.ifBlank { name },
            productId = primaryProduct.id,
            product = primaryProduct,
            products = products.map { it.product },
            initialWeightG = totalWeight,
            totalKcal = macros.totalKcal,
            totalProtein = macros.totalProtein,
            totalFat = macros.totalFat,
            totalCarbs = macros.totalCarbs
        )
    }
)

fun RecipeDto.toVirtualBatchMeal(): BatchMealDto {
    val segments = sections.mapIndexed { index, section ->
        val products = section.ingredients.map { ProductData(it.product, it.amount) }
        val totalWeight = products.sumOf { it.weightG }
        val macros = sectionMacrosFromProducts(products)
        val primary = products.firstOrNull()?.product
        BatchMealSegmentDto(
            id = index.toLong(),
            name = section.name,
            product = primary,
            initialWeightG = totalWeight,
            currentWeightG = totalWeight,
            totalKcal = macros.totalKcal,
            totalProtein = macros.totalProtein,
            totalFat = macros.totalFat,
            totalCarbs = macros.totalCarbs
        )
    }
    return BatchMealDto(
        id = -1,
        name = name,
        recipeId = id?.toLongOrNull(),
        isDepleted = false,
        segments = segments
    )
}

fun buildConsumeRequestsFromRecipe(
    recipe: RecipeDto,
    segmentWeights: Map<Long, Double>,
    mealDate: String,
    mealType: String
): List<ConsumeProductRequest> {
    val apiMealType = MealTypeMapper.toApi(mealType)
    val requests = mutableListOf<ConsumeProductRequest>()

    recipe.sections.forEachIndexed { index, section ->
        val takenWeight = segmentWeights[index.toLong()] ?: return@forEachIndexed
        if (takenWeight <= 0) return@forEachIndexed

        val sectionTotal = section.ingredients.sumOf { it.amount }
        if (sectionTotal <= 0) return@forEachIndexed

        val ratio = takenWeight / sectionTotal
        section.ingredients.forEach { ingredient ->
            val weight = ingredient.amount * ratio
            if (weight <= 0) return@forEach
            requests.add(
                ConsumeProductRequest(
                    product = ingredient.product,
                    weightG = weight,
                    mealDate = mealDate,
                    mealType = apiMealType
                )
            )
        }
    }

    return requests
}
