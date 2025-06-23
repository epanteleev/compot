rootProject.name = "compot-root"

pluginManagement {
    includeBuild("build-config")
}

include("opt")
include("examples")
include("compot")
include("opt-driver")
include("compot-driver")