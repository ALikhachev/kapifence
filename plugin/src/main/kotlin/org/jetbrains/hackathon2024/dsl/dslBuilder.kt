package org.jetbrains.hackathon2024.dsl

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.hackathon2024.ArgumentType
import org.jetbrains.hackathon2024.KapiFenceClassDsl
import org.jetbrains.hackathon2024.KapiFenceRootDsl

internal class KapiFenceRootBuilder(project: Project) : KapiFenceRootDsl {
    internal val records: List<String>
        get() = records_
    private val records_: MutableList<String> = mutableListOf()

    override val deprecationMessage: Property<String> = project.objects.property(String::class.java)

    override fun deprecateClass(
        name: String,
        body: (KapiFenceClassDsl.() -> Unit)?,
    ) {
        records_.add("class $name")
        if (body != null) {
            deprecateMembers(name, body)
        }
    }

    private fun String.maybeWrapIntoClass(className: String): String? {
        return if (isEmpty()) {
            null
        } else {
            "class $className {\n$this\n}"
        }
    }

    override fun deprecateMembers(
        name: String,
        body: KapiFenceClassDsl.() -> Unit,
    ) {
        val funBuilder = StringBuilder()
        val propBuilder = StringBuilder()
        KapiFenceClassBuilder(funBuilder, propBuilder).body()
        funBuilder.toString().maybeWrapIntoClass(name)?.let {
            records_.add(it)
        }
        propBuilder.toString().maybeWrapIntoClass(name)?.let {
            records_.add(it)
        }
    }
}

internal class KapiFenceClassBuilder(private val funBuilder: StringBuilder, private val propBuilder: StringBuilder) : KapiFenceClassDsl {
    override fun deprecateConstructor(vararg argumentType: ArgumentType) {
        deprecateFun("<init>", *argumentType)
    }

    override fun deprecateFun(
        name: String,
        vararg argumentType: ArgumentType,
    ) {
        funBuilder.appendLine("*** $name(${argumentType.joinToString(", ") { it.type }});")
    }

    override fun deprecateProperty(name: String) {
        propBuilder.appendLine("private *** $name;")
    }
}