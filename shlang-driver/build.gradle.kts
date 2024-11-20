import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("multiplatform") version "2.0.0"
    id("org.jetbrains.dokka") version "1.9.20"
    application
}

group = "org.shlang"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

application {
    mainClass.set("ShlangStartupKt")
}

kotlin {
    jvm {
        withJava()
    }
    linuxX64 {
        binaries {
            executable {
                baseName = "ShlangStartup"
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("com.squareup.okio:okio:3.9.0")
                implementation(project(":opt-driver"))
            }
        }
        commonMain {
            dependencies {
                implementation(project(":opt"))
                implementation(project(":shlang"))
            }
        }
        jvmTest {
            dependencies {
                implementation("junit:junit:4.13")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")

                implementation("org.jetbrains.kotlin:kotlin-scripting-common")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
            }
        }
    }
}

tasks.named<Jar>("jvmJar") {
    dependsOn.add(tasks.findByName("jvmTest"))
}

tasks.named<Jar>("jar") {
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        subprojects.flatMap { it.configurations.getByName("runtimeClasspath").files }
    })
}

tasks.withType(Test::class.java).all {
    jvmArgs("-ea")
    mkdir("test-results")
    environment("TEST_RESULT_DIR", "test-results")

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.create<Exec>("runtests") {
    dependsOn("installDist")
    project.logger.debug("Running tests")
    workingDir = workingDir.resolve("scripts")
    val shlangDriver = projectDir.resolve("build")
        .resolve("install")
        .resolve("shlang-driver")
        .resolve("bin")
        .resolve("shlang-driver")

    commandLine("python3", "bfish.py", shlangDriver)
    commandLine("python3", "chibicc.py", shlangDriver)
}