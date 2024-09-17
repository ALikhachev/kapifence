package org.jetbrains.hackathon2024.parser

import proguard.Configuration
import proguard.ConfigurationParser
import proguard.KeepClassSpecification

class ProguardParser {

    fun parse(pattern: String): List<KeepClassSpecification> {
        val configuration = Configuration()
        ConfigurationParser(arrayOf("-keep", pattern), null).parse(configuration)
        return configuration.keep
    }
}