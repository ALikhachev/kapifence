package org.jetbrains.hackathon2024.test

class ClassWithFieldToBeDeprecated {

    val propertyToBeDeprecated = ""
        @JvmName("getBread")
        get

    val propertyToBeDeprecatedWithGetter = ""

    internal val propertyToBeDeprecatedInternal = ""

    private val propertyNotDeprecated = ""
}