pluginManagement {
    repositories {
        val bridge = System.getenv("SELA_MAVEN_BRIDGE")
        if (bridge != null) { maven("$bridge/google") { isAllowInsecureProtocol = true }; maven("$bridge/central") { isAllowInsecureProtocol = true }; maven("$bridge/plugins") { isAllowInsecureProtocol = true } }
        else { google(); mavenCentral(); gradlePluginPortal() }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val bridge = System.getenv("SELA_MAVEN_BRIDGE")
        if (bridge != null) { maven("$bridge/google") { isAllowInsecureProtocol = true }; maven("$bridge/central") { isAllowInsecureProtocol = true } }
        else { google(); mavenCentral() }
    }
}
rootProject.name = "Sela"
include(":app", ":core:model", ":core:database", ":core:network", ":core:location", ":core:designsystem", ":feature:map", ":feature:search", ":feature:settings", ":feature:unity")
// Unity as a Library: sertakan hasil export Unity hanya jika direktori export ada.
// Lihat unity/EXPORT_GUIDE.md untuk cara menghasilkan unity-export/unityLibrary.
val unityLibDir = file("unity-export/unityLibrary")
if (unityLibDir.exists()) {
    include(":unityLibrary")
    project(":unityLibrary").projectDir = unityLibDir
}
