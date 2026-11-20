package app.studydesk.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.project

/** Every feature module: library + Compose + Hilt, plus the shared core modules and UI libraries. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("studydesk.android.library")
        pluginManager.apply("studydesk.android.compose")
        pluginManager.apply("studydesk.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        dependencies {
            "implementation"(project(":core:model"))
            "implementation"(project(":core:designsystem"))
            "implementation"(libs.findLibrary("androidx.core.ktx").get())
            "implementation"(libs.findLibrary("androidx.compose.ui").get())
            "implementation"(libs.findLibrary("androidx.compose.foundation").get())
            "implementation"(libs.findLibrary("androidx.compose.animation").get())
            "implementation"(libs.findLibrary("androidx.compose.material3").get())
            "implementation"(libs.findLibrary("androidx.compose.material.icons.extended").get())
            "implementation"(libs.findLibrary("androidx.navigation.compose").get())
            "implementation"(libs.findLibrary("androidx.lifecycle.runtime.compose").get())
            "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
            "implementation"(libs.findLibrary("hilt.ext.lifecycle.viewmodel.compose").get())
            "implementation"(libs.findLibrary("kotlinx.coroutines.core").get())
            "implementation"(libs.findLibrary("kotlinx.serialization.json").get())
        }
    }
}
