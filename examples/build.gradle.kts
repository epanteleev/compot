plugins {
    kotlin("jvm") version "2.0.0"
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

application {
    mainClass.set("examples.AnalysisKt")
}