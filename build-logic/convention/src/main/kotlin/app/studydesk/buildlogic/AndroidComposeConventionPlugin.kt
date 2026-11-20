package app.studydesk.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Compose compiler, the Compose BOM and debug tooling. Apply next to the library/application plugin. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.findByType(LibraryExtension::class.java)?.let { configureCompose(it) }
        extensions.findByType(ApplicationExtension::class.java)?.let { configureCompose(it) }
    }
}

private fun Project.configureCompose(extension: CommonExtension) {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
    extension.buildFeatures.compose = true
    dependencies {
        val bom = libs.findLibrary("androidx.compose.bom").get()
        "implementation"(platform(bom))
        "implementation"(libs.findLibrary("androidx.compose.ui.tooling.preview").get())
        "debugImplementation"(libs.findLibrary("androidx.compose.ui.tooling").get())
    }
}
