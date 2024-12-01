rootProject.name = "ssa-construction"

pluginManagement {
    includeBuild("build-config")
}

include("opt")
include("examples")
include("shlang")
include("opt-driver")
include("shlang-driver")