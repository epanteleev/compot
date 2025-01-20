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

tasks.withType(Test::class).all {
    dependsOn(":opt:jvmTest")
    dependsOn(":examples:build")

    jvmArgs("-ea")
    mkdir("test-results")
    environment("TEST_RESULT_DIR", "test-results")

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}