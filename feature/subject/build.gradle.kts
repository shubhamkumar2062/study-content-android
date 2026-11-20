plugins {
    alias(libs.plugins.studydesk.android.feature)
}

android {
    namespace = "app.studydesk.feature.subject"
}

dependencies {
    implementation(project(":core:content"))
    implementation(project(":core:database"))
    implementation(project(":core:reader"))
}
