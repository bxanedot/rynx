import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

group = "dev.bxane.rynx"
version = "0.1.0-dev"

kotlin {
    jvmToolchain(27)
    jvm { compilerOptions { jvmTarget = JvmTarget.JVM_26 } }
    android {
        namespace = "dev.bxane.rynx.vyonex"
        compileSdk = 37
        minSdk = 26
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmCommon") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }
    sourceSets {
        val jvmCommonMain by getting
        jvmCommonMain.dependencies {
            api(project(":shared:playback"))
            api("com.vyonex:vyonex:0.2.0")
            implementation("com.squareup.okhttp3:okhttp:5.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
        jvmTest.dependencies { implementation(kotlin("test")) }
    }
}

tasks.withType<Test>().configureEach {inputs.property("appleLiveContracts",providers.environmentVariable("RYNX_APPLE_LIVE").orElse("0"))}
tasks.withType<Test>().configureEach {inputs.property("youtubeLiveContracts",providers.environmentVariable("RYNX_YOUTUBE_LIVE").orElse("0"))}
