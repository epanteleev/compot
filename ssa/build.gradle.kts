import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("multiplatform") version "2.0.0"
    distribution
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
            sharedLib {
                baseName = "ssa"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
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