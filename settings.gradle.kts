pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://maven.minecraftforge.net/")
        maven(url = "https://jitpack.io/")
        maven(url = "https://repo.spongepowered.org/repository/maven-public/")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "net.minecraftforge.gradle.forge" ->
                    useModule("com.github.ccbluex:ForgeGradle:${extra["forgegradle_version"]}")
                "org.spongepowered.mixin" ->
                    useModule("com.github.xcfrg:mixingradle:${extra["mixingradle_version"]}")
            }
        }
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version extra["kotlin_version"] as String
    }
}

rootProject.name = "LibreBounce"