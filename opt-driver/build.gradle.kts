import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("kotlin-application")
}

group = "org.ssa"
version = "1.0"

application {
    mainClass.set("OptStartupKt")
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":opt"))
            }
        }
    }
}

tasks.named<Jar>("jar") {
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        subprojects.flatMap { it.configurations.getByName("runtimeClasspath").files }
    })
}

tasks.withType(Test::class).all {
    dependsOn(":opt:jvmTest")

    jvmArgs("-ea")
    mkdir("test-results")
    environment("TEST_RESULT_DIR", "test-results")

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}