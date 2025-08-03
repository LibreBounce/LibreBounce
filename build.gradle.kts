plugins {
    java
    idea
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("net.minecraftforge.gradle.forge") version "forge_gradle_version"
    id("org.spongepowered.mixin") version "mixin_gradle_version"
    id("com.gorylenko.gradle-git-properties") version "2.5.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://repo.spongepowered.org/repository/maven-public/")
    maven(url = "https://jitpack.io/")
}

val archivesBaseName: String by project
val modVersion: String by project
val mavenGroup: String by project
val kotlinVersion: String by project
val kotlinCoroutinesVersion: String by project

group = mavenGroup
version = modVersion

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

configure<net.minecraftforge.gradle.common.tasks.MinecraftForgeExtension> {
    version = "1.8.9-11.15.1.2318-1.8.9"
    runDir = "run"
    mappings = "stable_22"
    makeObfSourceJar = false
    clientJvmArgs.addAll(
        listOf(
            "-Dfml.coreMods.load=net.ccbluex.liquidbounce.injection.forge.MixinLoader",
            "-Xmx4096m",
            "-Xms1024m",
            "-Ddev-mode"
        )
    )
}

configurations {
    named("runtimeOnly") {
        isCanBeResolved = true
    }
}

dependencies {
    implementation("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
        exclude(module = "guava")
        exclude(module = "commons-io")
        exclude(module = "gson")
        exclude(module = "launchwrapper")
        exclude(module = "log4j-core")
        exclude(module = "slf4j-api")
    }

    annotationProcessor("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    implementation("com.jagrosh:DiscordIPC:0.4")

    implementation("com.github.CCBlueX:Elixir:1.2.6") {
        exclude(module = "kotlin-stdlib")
        exclude(module = "authlib")
    }

    implementation("com.github.TheAltening:TheAltening4j:d0771f42d3")
    implementation("com.github.TheAltening:API-Java-AuthLib:63a9702615")

    implementation("org.knowm.xchart:xchart:3.8.8")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14") {
        exclude(module = "kotlin-stdlib")
    }

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutinesVersion")

    // Update Checker - Semver Implementation
    implementation("org.semver4j:semver4j:5.7.1")

    // Swing LookAndFeel
    implementation("com.formdev:flatlaf:3.6.1")

    implementation(fileTree("libs") { include("*.jar") })
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    exclude("LICENSE.txt")
    exclude("META-INF/maven/**")
    exclude("META-INF/versions/**")
    exclude("org/apache/log4j/**")
    exclude("org/apache/commons/**")
    exclude("org/junit/**")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", project.extra["minecraft.version"])

    filesMatching("mcmod.info") {
        expand(mutableMapOf(
            "version" to project.version,
            "mcversion" to project.extra["minecraft.version"]
        ))
    }

    rename("(.+_at.cfg)", "META-INF/\$1")
}

val moveResources by tasks.registering(Copy::class) {
    from("${buildDir}/resources/main")
    into("${buildDir}/classes/java")
}

moveResources {
    dependsOn(tasks.processResources)
}
tasks.classes {
    dependsOn(moveResources)
}

tasks.jar {
    manifest {
        attributes(
            "FMLCorePlugin" to "net.ccbluex.liquidbounce.injection.forge.MixinLoader",
            "FMLCorePluginContainsFMLMod" to true,
            "ForceLoadAsMod" to true,
            "MixinConfigs" to "liquidbounce.forge.mixins.json",
            "ModSide" to "CLIENT",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "TweakOrder" to "0",
            "FMLAT" to "liquidbounce_at.cfg",
            "Main-Class" to "net.ccbluex.liquidinstruction.LiquidInstructionKt"
        )
    }
    enabled = false
}

configure<org.spongepowered.asm.gradle.plugins.MixinExtension> {
    disableRefMapWarning = true
    defaultObfuscationEnv = "searge"
    add(sourceSets.main.get(), "liquidbounce.mixins.refmap.json")
}

tasks.register<net.minecraftforge.gradle.tasks.ReobfTask>("reobfShadowJar") {
    mappingType = "SEARGE"
    mustRunAfter(tasks.named("shadowJar"))
}

tasks.jar {
    dependsOn(tasks.named("shadowJar"))
}

tasks.register<Copy>("copyZipInclude") {
    from("zip_include/")
    into("build/libs/zip")
}
tasks.build {
    dependsOn("copyZipInclude")
}