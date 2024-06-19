import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("multiplatform") version "2.0.0"
    distribution
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
            sharedLib {
                baseName = "ShlangDriver"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ssa"))
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("com.squareup.okio:okio:3.9.0")
            }
        }
        jvmMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-compiler")
            }
        }
        jvmTest {
            dependencies {
                implementation("junit:junit:4.13")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
    }
}

tasks.withType(Test::class.java).all {
    jvmArgs("-ea")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}