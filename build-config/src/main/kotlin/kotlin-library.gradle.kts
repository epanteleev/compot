
plugins {
    id("kotlin-common")
    id("org.jetbrains.dokka")
    distribution
}

tasks.withType(ProcessResources::class).all {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}