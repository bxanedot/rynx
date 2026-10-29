plugins { kotlin("multiplatform"); kotlin("plugin.compose"); id("org.jetbrains.compose"); id("com.android.kotlin.multiplatform.library") }
compose.resources {
    publicResClass = true
    packageOfResClass = "dev.bxane.rynx.ui.generated.resources"
}
kotlin {
    jvmToolchain(27)
    android { namespace = "dev.bxane.rynx.ui"; compileSdk = 37; minSdk = 26; compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }
    jvm { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(compose.components.resources)
            implementation(project(":glyphstudioSfsymbols"))
            api(project(":shared:playback"))
            api(project(":shared:library"))
            api(project(":shared:design"))
            implementation("dev.chrisbanes.haze:haze-glass:2.0.1")
            implementation("io.coil-kt.coil3:coil-compose:3.6.3")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.6.3")
        }
        jvmTest.dependencies { implementation(kotlin("test"));implementation(compose.desktop.currentOs) }
        androidMain.dependencies { implementation("androidx.activity:activity-compose:1.13.0") }
    }
}
