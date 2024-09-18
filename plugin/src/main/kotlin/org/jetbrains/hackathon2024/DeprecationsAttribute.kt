package org.jetbrains.hackathon2024

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPlugin
import java.io.Serializable

interface DeprecationsAttribute : Named, Serializable {
    companion object {
        @JvmField
        val ATTRIBUTE = Attribute.of(
            "org.jetbrains.hackathon2024.deprecations-status",
            DeprecationsAttribute::class.java
        )

        @JvmField
        val WITH_DEPRECATIONS = "with-deprecations"

        @JvmField
        val WITHOUT_DEPRECATIONS = "without-deprecations"
    }
}

internal fun Project.configureDeprecationsAttribute() {
    with(dependencies) {
        plugins.withId("java") {
            attributesSchema.attribute(DeprecationsAttribute.ATTRIBUTE)

            artifactTypes.getByName(ArtifactTypeDefinition.JAR_TYPE).attributes.attribute(
                DeprecationsAttribute.ATTRIBUTE,
                objects.named(
                    DeprecationsAttribute::class.java,
                    DeprecationsAttribute.WITHOUT_DEPRECATIONS
                )
            )

            project.configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).attributes.attribute(
                DeprecationsAttribute.ATTRIBUTE, objects.named(
                    DeprecationsAttribute::class.java,
                    DeprecationsAttribute.WITH_DEPRECATIONS
                )
            )
            project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).attributes.attribute(
                DeprecationsAttribute.ATTRIBUTE, objects.named(
                    DeprecationsAttribute::class.java,
                    DeprecationsAttribute.WITH_DEPRECATIONS
                )
            )
        }
    }
}

internal fun Project.registerDeprecationsTransform() {
    with(dependencies) {
        registerTransform(DeprecatingTransformAction::class.java) {
            it.from.attribute(
                DeprecationsAttribute.ATTRIBUTE,
                objects.named(
                    DeprecationsAttribute::class.java,
                    DeprecationsAttribute.WITHOUT_DEPRECATIONS
                )
            )
            it.to.attribute(
                DeprecationsAttribute.ATTRIBUTE,
                objects.named(
                    DeprecationsAttribute::class.java,
                    DeprecationsAttribute.WITH_DEPRECATIONS
                )
            )
        }
    }
}