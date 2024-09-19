![KapiFence](logo.png)

## Description
KapiFence is a Gradle plugin designed to help developers manage and deprecate outdated or unwanted code elements in their Kotlin projects, including those in external dependencies. This ensures that even dependencies from third-party libraries are properly noted as deprecated within your project.
### Features
* **Deprecate Classes**: Easily deprecate entire classes that are no longer needed.
* **Deprecate Constructors**: Mark specific constructors of classes as deprecated.
* **Deprecate Properties**: Highlight properties that should be avoided in further development.
* **Deprecate Functions**: Deprecate methods within your classes in a streamlined manner.
* **Wildcard Support**: Utilize wildcards (* and **) to deprecate multiple elements that match a pattern.
## How to work with the plugin

#### With Kotlin DSL in Gradle

1) Add KapiFence plugin repository to your `pluginManagement` block in `settings.gradle.kts` file:

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.pkg.github.com/ALikhachev/KapiFence") {
            credentials {
                username = "your_github_username"
                password = "your_github_token"
            }
        }
    }
}
```

[How to generate token on GitHub](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic). You need only the `read:packages` scope.

2) In `build.gradle.kts` add KapiFence plugin:

```koltin
plugins {
    id("org.jetbrains.hackathon2024.kapifence") version "0.0.2"
}
```

3) Describe what would you like to deprecate in `build.gradle.kts`:
```kotlin
kapiFence {
    deprecateClass("org.example.MyAwfulClass") // Deprecate the class `org.example.MyAwfulClass`
    deprecateMembers("org.example.MyAwesomeClass") {
        deprecateConstructor(ArgumentType.Int, ArgumentType.Int) // deprecate `MyAwesomeClass(int1: Int, int2: Int)` constructor
        deprecateProperty("oldAwfulProperty") // deprecate `oldAwfulProperty` property in the `org.example.MyAwesomeClass`
        deprecateFun("oldAwfulFun") // deprecate `oldAwfulFun` function with no arguments in the `org.example.MyAwesomeClass`
        deprecateFun("oldAwfulFun", ArgumentType.Class("org.example.MyAwfulClass")) // deprecate `oldAwfulFun` function overload
    }
    deprecateClass("org.example.OldBadClass") { // Deprecate the class `org.example.OldBadClass`
        deprecateConstructor(ArgumentType.Int, ArgumentType.Int) // deprecate `OldBadClass(int1: Int, int2: Int)` constructor
        deprecateProperty("oldAwfulProperty") // deprecate `oldAwfulProperty` property in the `org.example.OldBadClass`
        deprecateFun("oldAwfulFun") // deprecate `oldAwfulFun` function in the `org.example.OldBadClass`
        deprecateFun("oldFunWithOverloads", ArgumentType.Wildcard) // deprecate `oldFunWithOverloads` functions with any argument type
    }
}
```