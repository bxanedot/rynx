plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("app.cash.sqldelight")
}

kotlin {
    jvmToolchain(27)
    android {
        namespace = "dev.bxane.rynx.library"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
    jvm {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        }
    }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            api("app.cash.sqldelight:runtime:2.4.1")
            implementation("app.cash.sqldelight:coroutines-extensions:2.4.1")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.4.1")
        }
    }
}

sqldelight {
    databases {
        create("RynxLibraryDatabase") {
            packageName.set("dev.bxane.rynx.library.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
