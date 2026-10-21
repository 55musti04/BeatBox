// Clear conflicting ANDROID_PREFS_ROOT environment variable if ANDROID_USER_HOME is set
try {
    System.clearProperty("ANDROID_PREFS_ROOT")
    val processEnvClass = Class.forName("java.lang.ProcessEnvironment")
    val envField = processEnvClass.getDeclaredField("theEnvironment").apply { isAccessible = true }
    (envField.get(null) as? MutableMap<*, *>)?.remove("ANDROID_PREFS_ROOT")

    val ciEnvField = processEnvClass.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
    (ciEnvField.get(null) as? MutableMap<*, *>)?.remove("ANDROID_PREFS_ROOT")
} catch (_: Exception) {
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BeatBox"
include(":app")
