import groovy.json.JsonSlurper
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.studydesk.android.application)
    alias(libs.plugins.studydesk.android.compose)
    alias(libs.plugins.studydesk.hilt)
    alias(libs.plugins.kotlin.serialization)
}

val contentBaseUrl = "https://shubhamkumar2062.github.io/study-content/v1/"
val contentMirrorUrl = "https://cdn.jsdelivr.net/gh/shubhamkumar2062/study-content@published/v1/"

// Release signing details live in keystore.properties (git-ignored, written by
// scripts/create-release-key.sh). Without it, release builds come out unsigned.
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.isFile }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "app.studydesk"

    defaultConfig {
        applicationId = "app.studydesk"
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "CONTENT_BASE_URL", "\"$contentBaseUrl\"")
        buildConfigField("String", "CONTENT_MIRROR_URL", "\"$contentMirrorUrl\"")
        // The newest content format this build reads; the manifest's minAppVersion is compared with it.
        buildConfigField("int", "CONTENT_APP_VERSION", "1")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "SHOW_UNREVIEWED", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Release builds also open subjects a teacher hasn't checked yet; the app labels
            // them "Not yet checked" everywhere. Set to "false" to show only reviewed subjects.
            buildConfigField("boolean", "SHOW_UNREVIEWED", "true")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/INDEX.LIST", "/META-INF/*.version")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:content"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:reader"))
    implementation(project(":feature:home"))
    implementation(project(":feature:catalog"))
    implementation(project(":feature:subject"))
    implementation(project(":feature:practice"))
    implementation(project(":feature:exams"))
    implementation(project(":feature:search"))
    implementation(project(":feature:library"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.ext.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.androidx.profileinstaller)
    // Hilt 2.59.2 bundles a kotlin-metadata-jvm that reads metadata only up to Kotlin 2.3;
    // the Hilt aggregating step in the app runs as a Java annotation processor and needs 2.4.
    annotationProcessor("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
    implementation(libs.kotlinx.serialization.json)
}

/**
 * Copies the live content (text only, no images) into the app so a fresh install works
 * offline. Run before each release: ./gradlew :app:updateContentSnapshot
 * (or -PcontentDir=../study-content/public/v1 to bundle a local build)
 */
tasks.register("updateContentSnapshot") {
    group = "content"
    description = "Downloads the published content into src/main/assets/content/v1 and checks every file."
    val outDir = layout.projectDirectory.dir("src/main/assets/content/v1").asFile
    doLast {
        // -PcontentDir=<folder> copies a local build (study-content/public/v1) instead of the live site,
        // e.g. to bundle new content before it is published. Checksums are verified either way.
        val localDir = (project.findProperty("contentDir") as String?)?.let { rootProject.file(it) }
        fun download(path: String): ByteArray =
            if (localDir != null) localDir.resolve(path).readBytes() else uri(contentBaseUrl + path).toURL().readBytes()
        fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        val manifestBytes = download("manifest.json")
        @Suppress("UNCHECKED_CAST")
        val manifest = JsonSlurper().parse(manifestBytes) as Map<String, Any?>
        val entries = mutableListOf(manifest["catalog"] as Map<String, Any?>)
        (manifest["exams"] as List<Map<String, Any?>>).forEach { entries += it }
        (manifest["packs"] as List<Map<String, Any?>>).forEach { entries += it }

        val files = entries.associate { entry ->
            val path = entry["path"] as String
            val bytes = download(path)
            check(bytes.size.toLong() == (entry["bytes"] as Number).toLong() && sha256(bytes) == entry["sha256"]) {
                "$path does not match its manifest checksum; the site may be mid-deploy. Try again in a minute."
            }
            path to bytes
        }
        outDir.deleteRecursively()
        files.forEach { (path, bytes) -> outDir.resolve(path).apply { parentFile.mkdirs() }.writeBytes(bytes) }
        outDir.resolve("manifest.json").writeBytes(manifestBytes)
        println("Content snapshot: ${files.size} files, generated ${manifest["generatedAt"]}")
    }
}
