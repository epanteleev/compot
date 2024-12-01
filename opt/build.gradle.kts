import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("kotlin-library")
}

group = "org.opt"
version = "1.0"

kotlin {
    jvm()
}

tasks.withType(Test::class.java).all {
    jvmArgs("-ea")
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}