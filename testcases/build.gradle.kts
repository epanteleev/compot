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
    useJUnitPlatform()
}

sourceSets {
    test {
        resources {
            srcDir("src/test/resources")
        }
    }
}

kotlin {
    jvmToolchain(17)
}