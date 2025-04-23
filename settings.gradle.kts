pluginManagement {
    repositories {
        jcenter()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://jitpack.io")
//            url = uri("https://plugins.gradle.org/m2/")
        }
        maven {
            url = uri("https://mvnrepository.com/artifact/com.klinkerapps/android-smsmms")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        jcenter()
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
//            url = uri("https://plugins.gradle.org/m2/")
        }

    }
}

rootProject.name = "Messages"
include(":app")
 