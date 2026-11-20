plugins {
    alias(libs.plugins.studydesk.android.library)
    alias(libs.plugins.studydesk.android.compose)
}

android {
    namespace = "app.studydesk.core.designsystem"
}

dependencies {
    api(project(":core:content"))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
}
