plugins {
    alias(libs.plugins.studydesk.android.library)
    alias(libs.plugins.studydesk.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
}

android {
    namespace = "app.studydesk.core.database"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:content"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)
    api(libs.androidx.datastore.preferences)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
}

room {
    schemaDirectory("$projectDir/schemas")
}
