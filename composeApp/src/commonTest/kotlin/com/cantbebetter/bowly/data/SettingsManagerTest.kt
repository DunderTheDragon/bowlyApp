package com.cantbebetter.bowly.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsManagerTest {

    @Test
    fun clearSession_removesAuthButKeepsBaseUrl() {
        val manager = SettingsManager(MapSettings())
        manager.baseUrl = "http://192.168.1.10:8742"
        manager.token = "jwt"
        manager.username = "user"
        manager.role = "USER"

        manager.clearSession()

        assertEquals("http://192.168.1.10:8742", manager.baseUrl)
        assertTrue(manager.token.isNullOrBlank())
        assertTrue(manager.username.isNullOrBlank())
        assertTrue(manager.role.isNullOrBlank())
    }

    @Test
    fun clear_removesAllSettings() {
        val manager = SettingsManager(MapSettings())
        manager.baseUrl = "http://localhost:8742"
        manager.token = "jwt"

        manager.clear()

        assertTrue(manager.baseUrl.isNullOrBlank())
        assertTrue(manager.token.isNullOrBlank())
    }
}
