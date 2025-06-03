import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("kotlin-common")
    application
}

group = "org.shlang"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("ShlangStartupKt")
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation(project(":opt-driver"))
            }
        }
        commonMain {
            dependencies {
                implementation(project(":opt"))
                implementation(project(":opt-driver"))
                implementation(project(":shlang"))
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
    maxParallelForks = Runtime.getRuntime().availableProcessors() * 2 / 3

    dependsOn(":shlang:jvmTest")
    dependsOn(":opt-driver:jvmTest")

    jvmArgs("-ea")
    mkdir("test-results")
    environment("TEST_RESULT_DIR", "test-results")

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

private fun shlang(): String {
    val shlangDriver = projectDir.resolve("build")
        .resolve("install")
        .resolve("shlang-driver")
        .resolve("bin")
        .resolve("shlang-driver")

    return shlangDriver.toString()
}

task("makeDist") {
    group = "verification"
    dependsOn("test")
    dependsOn("installDist")
    project.logger.debug("Running tests")
}