package com.cantbebetter.bowly

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform