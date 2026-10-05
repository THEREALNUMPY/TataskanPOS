package com.tataskan.pos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
