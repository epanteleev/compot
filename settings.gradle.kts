

rootProject.name = "ssa-construction"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-multiplatform") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "kotlin-multiplatform") {
                useModule("org.jetbrains.kotlin:kotlin-klib-commonizer-embeddable:${requested.version}")
            }
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    plugins {
        id("org.jetbrains.dokka") version "1.9.20"
    }
}


include("opt")
include("examples")
include("shlang")
include("testcases")
include("opt-driver")
include("shlang-driver")