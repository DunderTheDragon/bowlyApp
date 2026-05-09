package com.cantbebetter.bowly.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiService(private val baseUrl: String, private val token: String? = null) {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        expectSuccess = true
    }

    private fun HttpRequestBuilder.auth() {
        token?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    suspend fun getStatus(): SystemStatusResponse {
        return client.get("$baseUrl/api/system/status").body()
    }

    suspend fun setupAdmin(request: SetupRequest): Boolean {
        val response = client.post("$baseUrl/api/system/setup") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    suspend fun login(request: LoginRequest): LoginResponse {
        return client.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getAdminKeys(): AdminKeysDto {
        return client.get("$baseUrl/api/system/admin/keys") {
            auth()
        }.body()
    }

    suspend fun saveAdminKeys(keys: AdminKeysDto): Boolean {
        val response = client.post("$baseUrl/api/system/admin/keys") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(keys)
        }
        return response.status.isSuccess()
    }

    suspend fun getUsers(): List<UserDto> {
        return client.get("$baseUrl/api/users") {
            auth()
        }.body()
    }

    suspend fun registerUser(request: RegisterRequest): Boolean {
        val response = client.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    suspend fun deleteUser(userId: Long): Boolean {
        val response = client.delete("$baseUrl/api/users/$userId") {
            auth()
        }
        return response.status.isSuccess()
    }

    // User Profile
    suspend fun getUserProfile(): UserDto {
        return client.get("$baseUrl/api/users/profile") {
            auth()
        }.body()
    }

    suspend fun updateUserProfile(user: UserDto): Boolean {
        val response = client.put("$baseUrl/api/users/profile") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(user)
        }
        return response.status.isSuccess()
    }

    // Products
    suspend fun searchProducts(query: String): List<ProductDto> {
        return client.get("$baseUrl/api/products/search") {
            auth()
            parameter("query", query)
        }.body()
    }

    suspend fun addLocalProduct(product: ProductDto): ProductDto {
        return client.post("$baseUrl/api/products/local") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(product)
        }.body()
    }

    // Batch Meals
    suspend fun getActiveBatchMeals(): List<BatchMealDto> {
        return client.get("$baseUrl/api/batch-meals/active") {
            auth()
        }.body()
    }

    suspend fun createBatchMeal(request: CreateBatchMealRequest): BatchMealDto {
        return client.post("$baseUrl/api/batch-meals") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun consumePortion(request: ConsumePortionRequest): Boolean {
        val response = client.post("$baseUrl/api/batch-meals/consume") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    suspend fun consumeProduct(request: ConsumeProductRequest): Boolean {
        val response = client.post("$baseUrl/api/diary/consume") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    suspend fun deleteConsumedMeal(id: String): Boolean {
        val response = client.delete("$baseUrl/api/diary/meals/$id") {
            auth()
        }
        return response.status.isSuccess()
    }

    suspend fun updateConsumedMeal(id: String, request: ConsumeProductRequest): Boolean {
        val response = client.put("$baseUrl/api/diary/meals/$id") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    // Recipes
    suspend fun getRecipes(): List<RecipeDto> {
        return client.get("$baseUrl/api/recipes") {
            auth()
        }.body()
    }

    suspend fun searchRecipes(query: String): List<RecipeDto> {
        return client.get("$baseUrl/api/recipes/search") {
            auth()
            parameter("query", query)
        }.body()
    }

    suspend fun saveRecipe(recipe: RecipeDto): RecipeDto {
        return client.post("$baseUrl/api/recipes") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(recipe)
        }.body()
    }

    suspend fun deleteRecipe(id: String): Boolean {
        val response = client.delete("$baseUrl/api/recipes/$id") {
            auth()
        }
        return response.status.isSuccess()
    }

    // Diary
    suspend fun getDailySummary(date: String): DailySummaryDto {
        return client.get("$baseUrl/api/diary/daily") {
            auth()
            parameter("date", date)
        }.body()
    }

    fun close() {
        client.close()
    }
}
