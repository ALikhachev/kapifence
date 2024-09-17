package org.jetbrains.hackathon2024.parser

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class ProguardParserTest {

    @Test
    fun parse() {
        val result = ProguardParser().parse("class com.example.MyClass { public * *(); }")
        assertEquals(1, result.size)
    }
}