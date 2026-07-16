import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    id("fabric-loom")
    id("ploceus")
}

version = "${providers.gradleProperty("mod_version").get()}+mc${providers.gradleProperty("minecraft_version").get()}"
group = providers.gradleProperty("maven_group").get()

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    mappings(ploceus.featherMappings(providers.gradleProperty("feather_build").get()))

    modImplementation("com.terraformersmc:modmenu:${providers.gradleProperty("mod_menu_version").get()}")

    ploceus.dependOsl(providers.gradleProperty("osl_version").get())

    implementation("com.jagrosh:DiscordIPC:0.4")

    implementation("com.github.CCBlueX:Elixir:1.2.6") {
        exclude module: "kotlin-stdlib"
        exclude module: "authlib"
    }

    implementation("com.github.TheAltening:TheAltening4j:d0771f42d3")
    implementation("com.github.TheAltening:API-Java-AuthLib:63a9702615")

    implementation("org.knowm.xchart:xchart:3.8.8")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.16") {
        exclude module: "kotlin-stdlib"
    }

    // Update Checker - Semver Implementation
    implementation("org.semver4j:semver4j:5.8.0")

    // Swing theme
    implementation("com.formdev:flatlaf:3.6.1")

    implementation fileTree(include: ["*.jar"], dir: "libs")
}

minecraft {
    version = "1.8.9-11.15.1.2318-1.8.9"
    runDir = "run"
    mappings = "stable_22"
    makeObfSourceJar = false
    clientJvmArgs += ["-Dfml.coreMods.load=net.ccbluex.liquidbounce.injection.forge.MixinLoader", "-Xmx4096m", "-Xms1024m", "-Ddev-mode"]
}

tasks.processResources {
    val version = version
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 8
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

java {
    // Still required by IDEs such as Eclipse and Visual Studio Code
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    // If this mod is going to be a library, then it should also generate Javadocs in order to aid with development.
    // Uncomment this line to generate them.
    // withJavadocJar()
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}

// configure the maven publication
publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}