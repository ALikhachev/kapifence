plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(libs.kotlin.test)
                // functionalTest test suite depends on the production code in tests
                implementation(project())
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) } 
                }
            }
        }
    }
}

gradlePlugin {
    plugins {
        register("fence") {
            id = "org.jetbrains.hackathon2024.kapifence"
            implementationClass = "org.jetbrains.hackathon2024.KapiFencePlugin"
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}
