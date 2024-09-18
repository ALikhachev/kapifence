package org.jetbrains.hackathon2024.test

class ClassWithMethodToBeDeprecated {

    fun methodToBeDeprecated() {}

    internal fun methodToBeDeprecatedInternal() {}

    private fun methodNotDeprecated() {}

    @Deprecated("FAKE")
    val deprecatedField: String = ""
        @JvmName("getBread")
        get
}