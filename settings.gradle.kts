pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    plugins {
        id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    }

}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "Nexus"
// core
include(":nexus-core")
include(":nexus-nms")
include(":nexus-nms:v1_21_R8")
include(":nexus-nms:v1_21_R7")
