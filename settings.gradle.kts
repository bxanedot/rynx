pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.TeamNewPipe")
                includeGroup("com.github.MetrolistGroup.innertubex")
                includeGroup("com.github.MetrolistGroup.quackjs")
            }
        }
        mavenCentral()
        google()
    }
}
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
include(":providers:innertubex")
include(":apps:android")
include(":providers:apple")
