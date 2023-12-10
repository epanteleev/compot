plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "org.ssa"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("startup.StartupKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    dependsOn.add(tasks.findByName("test"))
}