rootProject.name = "shlang-root"

pluginManagement {
    includeBuild("build-config")
}

include("opt")
include("examples")
include("shlang")
include("opt-driver")
include("shlang-driver")