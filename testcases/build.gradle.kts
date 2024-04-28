plugins {
    kotlin("jvm") version "1.9.21"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation(project(":ssa"))
    implementation(project(":shlang"))
}

tasks.test {
    jvmArgs("-ea")
    mkdir("test-results")
    environment("TEST_RESULT_DIR", "test-results")
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

kotlin {
    jvmToolchain(17)
}