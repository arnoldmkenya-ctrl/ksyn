package com.softsynth.ksyn

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform