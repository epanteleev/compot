rootProject.name = "ssa-construction"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
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
        }
    }

    plugins {
        kotlin("multiplatform") version "2.1.0"
        id("org.jetbrains.dokka") version "1.9.20"
    }
}


include("opt")
include("examples")
include("shlang")
include("opt-driver")
include("shlang-driver")