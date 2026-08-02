plugins {
    java
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    id("net.fabricmc.fabric-loom-remap") version("1.17.+")
    id("ploceus") version("1.17.+")
}

group = "dev.rdh"
version = "0.1"

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

@Suppress("MayBeConstant")
object Versions {
    val minecraft = "1.8.9"
    val feather = "1"
    val osl = "0.20.3"
    val fabric = "0.19.3"
}

ploceus {
    setIntermediaryGeneration(2)
}

repositories {
    maven("https://maven.taumc.org/releases")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://jitpack.io/")
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    mappings(ploceus.featherMappings(Versions.feather))

    modImplementation("net.fabricmc:fabric-loader:${Versions.fabric}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    implementation("com.jagrosh:DiscordIPC:0.4")

    implementation("com.github.CCBlueX:Elixir:1.2.6") {
        exclude(module = "kotlin-stdlib")
        exclude(module = "authlib")
    }

    /*implementation("com.github.TheAltening:TheAltening4j:d0771f42d3")
    implementation("com.github.TheAltening:API-Java-AuthLib:63a9702615")*/

    implementation("org.knowm.xchart:xchart:3.8.8")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.16") {
        exclude(module = "kotlin-stdlib")
    }

    // Update Checker - Semver Implementation
    implementation("org.semver4j:semver4j:5.8.0")

    // Swing theme
    implementation("com.formdev:flatlaf:3.6.1")

    ploceus.dependOsl(Versions.osl)
}

tasks.assemble {
    dependsOn("remapJar")
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}

tasks.processResources {
    val v = project.version
    inputs.property("version", v)

    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }
}