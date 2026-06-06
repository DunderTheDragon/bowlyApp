package com.cantbebetter.bowly.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ApiServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun serviceWithEngine(engine: MockEngine): ApiService {
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            expectSuccess = true
        }
        return ApiService("http://test.local", null, client)
    }

    @Test
    fun getStatus_parsujeOdpowiedzSystemu() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"isSetup":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val service = serviceWithEngine(engine)

        val status = service.getStatus()

        assertEquals(true, status.isSetup)
    }

    @Test
    fun login_zwracaToken() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/auth/login" -> respond(
                    content = """{"token":"abc","username":"jan","role":"USER","message":"ok"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val service = serviceWithEngine(engine)

        val response = service.login(LoginRequest("jan", "secret12"))

        assertEquals("abc", response.token)
        assertEquals("jan", response.username)
    }
}
