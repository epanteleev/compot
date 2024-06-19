plugins {
    kotlin("multiplatform") version "2.0.0"
    application
}

group = "org.shlang"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64 {
        binaries {
            executable {
                baseName = "ShlangDriver"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ssa"))
                implementation(project(":shlang"))
            }
        }
    }
}