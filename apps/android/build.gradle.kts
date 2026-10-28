plugins { id("com.android.application"); kotlin("plugin.compose"); id("org.jetbrains.compose") }
abstract class PackageLicenseAssets:DefaultTask() {
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val licenseFiles:ConfigurableFileCollection
    @get:OutputDirectory abstract val outputDirectory:DirectoryProperty
    @TaskAction fun packageNotices() {
        val destination=outputDirectory.get().asFile
        destination.deleteRecursively();destination.mkdirs()
        licenseFiles.files.forEach {source->
            if(source.isDirectory)source.copyRecursively(destination,overwrite=true)
            else source.copyTo(destination.resolve(source.name),overwrite=true)
        }
    }
}
val licenseAssets=tasks.register<PackageLicenseAssets>("packageLicenseAssets") {
    licenseFiles.from(rootProject.file("third_party_licenses"),rootProject.file("LICENSE"),rootProject.file("NOTICE"))
    outputDirectory.set(layout.buildDirectory.dir("generated/rynx-notices"))
}
androidComponents.onVariants {variant->
    variant.sources.assets?.addGeneratedSourceDirectory(licenseAssets){it.outputDirectory}
}
android {
    namespace = "dev.bxane.rynx.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.bxane.rynx"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    implementation(project(":shared:ui"))
    implementation(project(":shared:playback"))
    implementation(project(":providers:vyonex"))
    implementation(project(":providers:newpipe"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-datasource:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("io.ktor:ktor-client-okhttp:3.6.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation(compose.foundation)
}
