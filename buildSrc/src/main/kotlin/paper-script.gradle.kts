plugins {
    kotlin("jvm")
    id("xyz.jpenilla.run-paper")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("de.miraculixx:kpaper:1.2.1")
    compileOnly("dev.jorel:commandapi-paper-core:11.1.0")
    implementation("dev.jorel:commandapi-kotlin-paper:11.1.0")
    implementation("dev.jorel:commandapi-kotlin-core:11.1.0")
    implementation("dev.jorel:commandapi-paper-shade:11.1.0")
}

tasks.runServer {
    minecraftVersion("1.21.11")
}
