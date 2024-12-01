
dependencyResolutionManagement {
    // Reuse version catalog from the main build.
    versionCatalogs {
        create("libs") { from(files("libs.versions.toml")) }
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
}

rootProject.name = "build-config"