import org.jetbrains.compose.desktop.application.dsl.TargetFormat
plugins { kotlin("multiplatform"); kotlin("plugin.compose"); id("org.jetbrains.compose") }
kotlin {
    jvmToolchain(27)
    jvm { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_26 } }
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared:playback"))
            implementation(project(":shared:ui"))
            implementation(project(":providers:apple"))
            implementation(project(":providers:innertubex"))
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("io.ktor:ktor-client-cio:3.6.0")
        }
        jvmTest.dependencies { implementation(kotlin("test")) }
    }
}
compose.desktop {
    application {
        mainClass = "dev.bxane.rynx.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "rynx"
            packageVersion = "0.1.0"
            vendor = "bxane"
            modules("java.desktop")
        }
    }
}
