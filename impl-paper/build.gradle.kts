
plugins {
    `kotlin-script`
    `paper-script`
    `shadow-script`
}

dependencies {
//    implementation(project(":vanilla"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    main {
        resources.srcDirs("$rootDir/commons/")
    }
}

group = "de.miraculixx.bmbm"
setProperty("module_name", "bmbm")