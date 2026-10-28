plugins { kotlin("multiplatform"); id("com.android.kotlin.multiplatform.library") }
kotlin {
    jvmToolchain(27)
    android { namespace = "dev.bxane.rynx.design"; compileSdk = 37; minSdk = 26; compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }
    jvm { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }
    sourceSets { commonTest.dependencies { implementation(kotlin("test")) } }
}
