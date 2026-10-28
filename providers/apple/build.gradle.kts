plugins { kotlin("multiplatform"); id("com.android.kotlin.multiplatform.library") }
kotlin {
    jvmToolchain(27)
    android { namespace="dev.bxane.rynx.apple";compileSdk=37;minSdk=26;compilerOptions {jvmTarget=org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17} }
    jvm {compilerOptions {jvmTarget=org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21}}
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:playback"))
            api("io.ktor:ktor-client-core:3.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
        commonTest.dependencies {implementation(kotlin("test"));implementation("io.ktor:ktor-client-mock:3.6.0")}
        jvmTest.dependencies {implementation("io.ktor:ktor-client-okhttp:3.6.0")}
    }
}

tasks.withType<Test>().configureEach {inputs.property("appleLiveContracts",providers.environmentVariable("RYNX_APPLE_LIVE").orElse("0"))}
