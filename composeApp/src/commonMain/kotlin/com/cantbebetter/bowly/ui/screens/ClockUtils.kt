package com.cantbebetter.bowly.ui.screens

import kotlin.random.Random

// Wspólny mock zegara dla KMP
object Clock {
    private var lastNow: Long = 1715424000000L

    fun now(): Long {
        lastNow += 1
        return lastNow
    }

    // Dla ID wymagających większej unikalności w krótkim czasie
    fun uniqueId(): String {
        return "${now()}_${Random.nextInt(1000, 9999)}"
    }
    
    object System {
        fun now(): Instant = Instant(Clock.now())
    }
}

class Instant(val millis: Long) {
    fun toEpochMilliseconds(): Long = millis
}
