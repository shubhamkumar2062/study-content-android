package app.studydesk.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** compileSdk, minSdk and Java level shared by the app and every library module. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
    commonExtension.apply {
        compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
        defaultConfig.minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
        testOptions.unitTests.isReturnDefaultValues = true
        testOptions.unitTests.isIncludeAndroidResources = true
    }
}
