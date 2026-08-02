pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.ornithemc.net/releases")
        maven("https://maven.ornithemc.net/snapshots")
        maven("https://jitpack.io/")
        mavenCentral()
    }
}

// Should match your modid
rootProject.name = "librebounce"