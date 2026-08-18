pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "web-viability-spike"

include("core")
include("desktop")
include("web")
include("threadprobe")
include("collisionbench")
include("langprobe")
