plugins {
    alias(libs.plugins.studydesk.android.library)
    alias(libs.plugins.studydesk.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.studydesk.core.content"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    testImplementation(libs.ktor.client.mock)
}
