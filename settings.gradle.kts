pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement { repositories { mavenCentral(); google(); maven("https://jitpack.io") } }
includeBuild("dependencies/vyonex-clean") {
    dependencySubstitution {
        substitute(module("com.vyonex:vyonex")).using(project(":"))
    }
}

rootProject.name = "rynx"
include(":shared:playback")
include(":shared:design")
include(":shared:ui", ":apps:desktop", ":providers:vyonex")
include(":providers:newpipe")
include(":apps:android")
include(":providers:apple")
