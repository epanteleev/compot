plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ssa"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("examples.AnalysisKt")
}