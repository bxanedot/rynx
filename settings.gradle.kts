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
rootProject.name = "rynx"
include(":shared:playback")
include(":shared:library")
include(":shared:design")
include(":shared:ui", ":app:desktop")
include(":providers:newpipe")
include(":providers:innertubex")
include(":app:android")
include(":providers:apple")
include(":glyphstudioSfsymbols")
project(":glyphstudioSfsymbols").projectDir = file("dependencies/GlyphStudio/sfsymbols")
