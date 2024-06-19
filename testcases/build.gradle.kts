import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("multiplatform") version "2.0.0"
    application
}

group = "org.example"
version = "unspecified"


repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64 {
        binaries {
            executable()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":shlang"))
                implementation(project(":ssa"))
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
        jvmMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-compiler")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
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
    mkdir("test-results")
    environment("TEST_RESULT_DIR", "test-results")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}