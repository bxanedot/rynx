plugins { kotlin("multiplatform"); kotlin("plugin.compose"); id("org.jetbrains.compose") }
kotlin {
    jvmToolchain(21)
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}
