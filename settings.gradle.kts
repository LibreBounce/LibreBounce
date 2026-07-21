pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Ornithe Releases"
            url = uri("https://maven.ornithemc.net/releases")
        }
        maven {
            name = "Ornithe Snapshots"
            url = uri("https://maven.ornithemc.net/snapshots")
        }
        maven {
            name = "Jitpack"
            url = uri("https://jitpack.io/")
        }
        mavenCentral()
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            switch (requested.id.id) {
                case "net.minecraftforge.gradle.forge":
                    useModule("com.github.ccbluex:ForgeGradle:${forgegradle_version}")
                    break
                case "org.spongepowered.mixin":
                    useModule("com.github.xcfrg:mixingradle:${mixingradle_version}")
                    break
            }
        }
    }

    plugins {
        id("fabric-loom") version providers.gradleProperty("loom_version")
        id("ploceus") version providers.gradleProperty("ploceus_version")
    }
}

// Should match your modid
rootProject.name = "librebounce"