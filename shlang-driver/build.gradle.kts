plugins {
    kotlin("multiplatform") version "2.0.0"
    id("org.jetbrains.dokka") version "1.9.20"
    application
}

group = "org.shlang"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

application {
    mainClass.set("ShlangStartupKt")
}

kotlin {
    jvm {
        withJava()
    }
    linuxX64 {
        binaries {
            executable {
                baseName = "ShlangStartup"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":opt"))
                implementation(project(":shlang"))
            }
        }
    }
}

tasks.named<Jar>("jar") {
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        subprojects.flatMap { it.configurations.getByName("runtimeClasspath").files }
    })
}