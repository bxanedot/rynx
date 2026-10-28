plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

group = "dev.bxane.rynx"
version = "0.1.0-dev"

kotlin {
    jvmToolchain(27)
    android {
        namespace = "dev.bxane.rynx.newpipe"
        compileSdk = 37
        minSdk = 26
        compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
    }
    sourceSets {
        androidMain.dependencies {
            api(project(":providers:vyonex"))
            implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5") {
                // The extractor calls Rhino's Android-safe core API directly; this optional
                // JSR-223 adapter requires java.beans/javax.script, which Android does not ship.
                exclude(group = "org.mozilla", module = "rhino-engine")
            }
            implementation("com.squareup.okhttp3:okhttp:5.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
    }
}
