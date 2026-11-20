plugins {
    alias(libs.plugins.studydesk.android.feature)
}

android {
    namespace = "app.studydesk.core.reader"
}

dependencies {
    implementation(project(":core:content"))
    implementation(project(":core:database"))
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.coil.compose)
    implementation(libs.androidx.activity.compose)
}
