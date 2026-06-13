plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    fun pluginDep(id: String, version: String) = "${id}:${id}.gradle.plugin:${version}"
    val kotlinVersion = "2.1.0"

    compileOnly(kotlin("gradle-plugin", kotlinVersion))
    runtimeOnly(kotlin("gradle-plugin", kotlinVersion))
    compileOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
    runtimeOnly(pluginDep("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation(pluginDep("xyz.jpenilla.run-paper", "3.0.0"))
    implementation(pluginDep("com.gradleup.shadow", "9.2.2"))
}
kotlin {
    jvmToolchain(21)
}
