import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("multiplatform") version "2.0.0"
    id("org.jetbrains.dokka") version "1.9.20"
    distribution
}

group = "org.opt"
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
                baseName = "opt"
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

tasks.named<Jar>("jvmJar") {
    dependsOn.add(tasks.findByName("jvmTest"))
}