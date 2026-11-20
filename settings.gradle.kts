pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StudyDesk"
// Modules are added here as they are created (Gradle 9 rejects missing folders).
include(":core:model")
include(":core:content")
include(":core:database")
include(":core:designsystem")
include(":feature:catalog")
include(":feature:home")
include(":feature:settings")
include(":feature:subject")
include(":feature:exams")
include(":feature:search")
include(":feature:library")
include(":app")
include(":core:reader")
include(":feature:practice")
