package app.studydesk.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** AGP 9 has built-in Kotlin, so the Android library plugin alone enables Kotlin. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> { configureKotlinAndroid(this) }
        // Modules without tests yet should not fail `testDebugUnitTest` (Gradle 9 default).
        tasks.withType(Test::class.java).configureEach { failOnNoDiscoveredTests.set(false) }
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        dependencies {
            "testImplementation"(libs.findLibrary("junit").get())
            "testImplementation"(libs.findLibrary("kotlinx.coroutines.test").get())
        }
    }
}
