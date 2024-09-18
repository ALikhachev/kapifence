package org.jetbrains.hackathon2024

import org.gradle.api.provider.Property

@DslMarker
annotation class KapiFenceDsl

@KapiFenceDsl
sealed class ArgumentType(val type: kotlin.String) {

    object Byte : ArgumentType("byte")
    object NullableByte : ArgumentType("java.lang.Byte")
    object Short : ArgumentType("short")
    object NullableShort : ArgumentType("java.lang.Short")
    object Int : ArgumentType("int")
    object NullableInt : ArgumentType("java.lang.Integer")
    object Long : ArgumentType("long")
    object NullableLong : ArgumentType("java.lang.Long")
    object Float : ArgumentType("float")
    object NullableFloat : ArgumentType("java.lang.Float")
    object Double : ArgumentType("double")
    object NullableDouble : ArgumentType("java.lang.Double")
    object Char : ArgumentType("char")
    object NullableChar : ArgumentType("java.lang.Character")
    object Boolean : ArgumentType("boolean")
    object NullableBoolean : ArgumentType("java.lang.Boolean")
    object String : ArgumentType("java.lang.String")
    object Wildcard : ArgumentType("***")
    class Class(name: kotlin.String) : ArgumentType(name)
}

@KapiFenceDsl
interface KapiFenceRootDsl {
    val deprecationMessage: Property<String>
    fun deprecateClass(name: String, body: (KapiFenceClassDsl.() -> Unit)? = null)
    fun deprecateMembers(name: String, body: KapiFenceClassDsl.() -> Unit)
}

const val KAPI_FENCE_WILDCARD_NAME = "*"

@KapiFenceDsl
interface KapiFenceClassDsl {
    fun deprecateConstructor(vararg argumentType: ArgumentType)
    fun deprecateFun(name: String, vararg argumentType: ArgumentType)
    fun deprecateProperty(name: String)
}