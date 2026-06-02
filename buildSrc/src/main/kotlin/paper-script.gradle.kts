plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
}

repositories {
    mavenCentral()
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    compileOnly("de.miraculixx:kpaper:1.2.1")
    compileOnly("dev.jorel:commandapi-paper-core:11.2.0")
    implementation("dev.jorel:commandapi-kotlin-paper:11.2.0")
    implementation("dev.jorel:commandapi-kotlin-core:11.2.0")
    implementation("dev.jorel:commandapi-paper-shade:11.2.0")
}

paperweight {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
