plugins {
    kotlin("multiplatform") version "2.0.0"
    application
}

group = "org.example"
version = "unspecified"


repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo1.maven.org/maven2/") }
    maven {
        url = uri("https://repo.spring.io/release")
    }
    maven {
        url = uri("https://repository.jboss.org/maven2")
    }
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
}

kotlin {
    jvm {

    }
    linuxX64 {
        binaries {
            executable()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ssa"))
                implementation(project(":shlang"))
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
        jvmMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-compiler")
            }
        }
        jvmTest {
            dependencies {
                implementation("junit:junit:4.13")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
    }
}

tasks.test {
    jvmArgs("-ea")
    mkdir("test-results")
    environment("TEST_RESULT_DIR", "test-results")
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
    }
}