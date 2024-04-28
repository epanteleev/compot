plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "ssa-construction"

include("ssa")
include("examples")
include("shlang")
include("testscases")
include("testcases")
include("testcases")