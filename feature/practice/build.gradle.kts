plugins {
    alias(libs.plugins.studydesk.android.feature)
}

android {
    namespace = "app.studydesk.feature.practice"
}

dependencies {
    implementation(project(":core:content"))
    implementation(project(":core:database"))
}
