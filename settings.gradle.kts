pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement { repositories { mavenCentral(); google() } }
rootProject.name = "rynx"
include(":shared:playback")
include(":shared:ui", ":apps:desktop")
