plugins {
    kotlin("multiplatform") version "2.0.0"
    application
}

group = "org.ssa"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64 {
        binaries {
            executable {
                baseName = "OptStartup"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ssa"))
            }
        }
    }
}