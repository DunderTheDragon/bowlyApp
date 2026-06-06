package com.cantbebetter.bowly.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

class SettingsManager(private val settings: Settings = Settings()) {
    var baseUrl: String?
        get() = settings["BASE_URL"]
        set(value) {
            settings["BASE_URL"] = value ?: ""
        }

    var token: String?
        get() = settings["JWT_TOKEN"]
        set(value) {
            settings["JWT_TOKEN"] = value ?: ""
        }

    var username: String?
        get() = settings["USERNAME"]
        set(value) {
            settings["USERNAME"] = value ?: ""
        }

    var role: String?
        get() = settings["ROLE"]
        set(value) {
            settings["ROLE"] = value ?: ""
        }

    // Goals and Profile
    var targetCalories: Double
        get() = settings.getDouble("TARGET_CALORIES", 2000.0)
        set(value) = settings.set("TARGET_CALORIES", value)

    var targetProtein: Double
        get() = settings.getDouble("TARGET_PROTEIN", 150.0)
        set(value) = settings.set("TARGET_PROTEIN", value)

    var targetFat: Double
        get() = settings.getDouble("TARGET_FAT", 65.0)
        set(value) = settings.set("TARGET_FAT", value)

    var targetCarbs: Double
        get() = settings.getDouble("TARGET_CARBS", 200.0)
        set(value) = settings.set("TARGET_CARBS", value)

    fun clear() {
        settings.clear()
    }

    fun clearSession() {
        token = null
        username = null
        role = null
    }
}
