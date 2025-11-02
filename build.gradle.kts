
plugins {
    id("java-library")
    id("com.gradleup.shadow") version "8.3.5"
    id("maven-publish")
    id("java")
    id("io.papermc.paperweight.userdev") apply false

}

dependencies {
    implementation(project(":nexus-core"))
    implementation(project(":nexus-nms:v1_21_R7"))
    implementation(project(":nexus-nms:v1_21_R8"))
    implementation(project(":nexus-nms:v1_21_R9"))
    implementation(project(":nexus-nms:v1_21_R10"))

}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")

    repositories {
        mavenCentral()
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }
        // Paper
        maven("https://repo.papermc.io/repository/maven-public/")
        // PlaceholderAPI
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        // WorldGuard
        maven("https://maven.enginehub.org/repo/")
        // MythicMobs
        maven("https://mvn.lumine.io/repository/maven-public/")
        // Nexo
        maven("https://repo.nexomc.com/releases")
        // Codemc
        maven("https://repo.codemc.org/repository/maven-public")
        // aikar
        maven("https://repo.aikar.co/content/groups/aikar/")
        // MMOItems
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
        //Eco Series
        maven("https://repo.auxilor.io/repository/maven-public/")
    }

    dependencies {
        // Adventure
        implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    }


    tasks {

        compileJava {
            options.encoding = "UTF-8"
        }

        build {
            dependsOn(shadowJar)
        }
    }

    java {
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

tasks {
    shadowJar {
        relocate("co.aikar.commands", "io.nexstudios.nexus.libs.commands")
        relocate("co.aikar.locales", "io.nexstudios.nexus.libs.locales")
    }
}

group = "io.nexstudios"
version = project.property("version") as String
