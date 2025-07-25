pluginManagement {
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        gradlePluginPortal()
    }
}
rootProject.name = "Nexus"
// core
include(":nexus-core")
