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
            url = "https://jitpack.io/"
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("fabric-loom") version providers.gradleProperty("loom_version")
        id("ploceus") version providers.gradleProperty("ploceus_version")
    }
}

// Should match your modid
rootProject.name = "template-mod"