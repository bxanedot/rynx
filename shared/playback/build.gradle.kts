plugins { kotlin("multiplatform"); id("com.android.kotlin.multiplatform.library") }
kotlin {
    jvmToolchain(27)
    android { namespace = "dev.bxane.rynx.playback"; compileSdk = 37; minSdk = 26; compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }
    jvm { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
            implementation("io.ktor:ktor-client-core:3.6.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:3.6.0")
        }
    }
}
