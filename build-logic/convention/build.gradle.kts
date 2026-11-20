plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // APIs only; the real plugins come from the root build's `apply false` entries.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "studydesk.android.application"
            implementationClass = "app.studydesk.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "studydesk.android.library"
            implementationClass = "app.studydesk.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "studydesk.android.compose"
            implementationClass = "app.studydesk.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "studydesk.android.feature"
            implementationClass = "app.studydesk.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = "studydesk.hilt"
            implementationClass = "app.studydesk.buildlogic.HiltConventionPlugin"
        }
    }
}
