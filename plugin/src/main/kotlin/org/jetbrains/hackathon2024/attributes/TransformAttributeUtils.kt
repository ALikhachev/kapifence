package org.jetbrains.hackathon2024.attributes

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.jetbrains.hackathon2024.transformations.DependencyTransformAction

val attributeForTransformation = Attribute.of("org.jetbrains.hackathon2024.transform", String::class.java)

fun Project.setUpAttribute() {
    dependencies.attributesSchema.attribute(attributeForTransformation)
    dependencies.artifactTypes.getByName("jar").attributes.attribute(attributeForTransformation, "without_deprecations")
    dependencies.registerTransform(DependencyTransformAction::class.java) {
        it.from.attribute(attributeForTransformation, "without_deprecations")
        it.to.attribute(attributeForTransformation, "with_deprecations")
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        project.configurations.getByName("compileClasspath").attributes.attribute(attributeForTransformation, "with_deprecations")
        project.configurations.getByName("runtimeClasspath").attributes.attribute(attributeForTransformation, "with_deprecations")
    }
}

