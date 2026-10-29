plugins {
    id("com.android.application") version "9.4.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.4.1" apply false
    kotlin("multiplatform") version "2.4.21" apply false
    kotlin("plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.12.1" apply false
    id("app.cash.sqldelight") version "2.4.1" apply false
}
group = "dev.bxane.rynx"
version = "0.1.0-dev"

// Rynx consumes GlyphStudio's dynamic SfSymbol/SfSymbolIcon API. Omit its optional 14k generated
// per-symbol wrapper source files; the dynamic catalog and full SF geometry data remain intact.
subprojects {
    if (path == ":glyphstudioSfsymbols") {
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                sourceSets.named("commonMain") {
                    kotlin.exclude("com/composables/sfsymbols/dualtone/**")
                    kotlin.exclude("com/composables/sfsymbols/monochrome/**")
                }
            }
        }
    }
}
