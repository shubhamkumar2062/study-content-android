plugins {
    alias(libs.plugins.studydesk.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.studydesk.core.model"
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
