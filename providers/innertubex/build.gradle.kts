import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

group = "dev.bxane.rynx"
version = "0.1.0-dev"

kotlin {
    jvmToolchain(27)
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    android {
        namespace = "dev.bxane.rynx.innertubex"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:playback"))
            api(project(":providers:apple"))
            implementation("com.github.MetrolistGroup.innertubex:innertubex:v0.11.2")
            implementation("io.ktor:ktor-client-core:3.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
