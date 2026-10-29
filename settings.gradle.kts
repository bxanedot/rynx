pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement { repositories { mavenCentral(); google(); maven("https://jitpack.io") } }
includeBuild("dependencies/eXcor") {
    dependencySubstitution {
        substitute(module("com.bxane.excor:excor")).using(project(":"))
    }
}

rootProject.name = "rynx"
include(":shared:playback")
include(":shared:library")
include(":shared:design")
include(":shared:ui", ":apps:desktop", ":providers:excor")
include(":providers:newpipe")
include(":apps:android")
include(":providers:apple")
