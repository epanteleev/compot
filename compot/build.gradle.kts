import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("kotlin-library")
}

group = "org.compot"
version = "1.0-SNAPSHOT"


kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":opt"))
            }
            resources.srcDirs("src/commonMain/resources")
        }
    }
}

tasks.withType(Test::class).all {
    dependsOn(":opt:jvmTest")

    jvmArgs("-ea")
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}