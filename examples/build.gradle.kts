plugins {
    kotlin("jvm") version "2.0.0"
    application
}

group = "org.example"
version = "1.0"

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":opt"))
}

application {
    mainClass.set("examples.AnalysisKt")
}