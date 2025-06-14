plugins {
    id("java")
    kotlin("jvm") version "1.8.0" // Ensure you have the Kotlin plugin applied
    id("io.freefair.lombok") version "8.11"
    id("com.gradleup.shadow") version "8.3.5"
    id("maven-publish")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "io.lightstudios.nexus.bukkit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "codemc"
        url = uri("https://repo.codemc.org/repository/maven-public")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    maven {
        name = "worldguard"
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        name = "nexo"
        url = uri("https://repo.nexomc.com/releases")
    }
    maven {
        name = "mythicmobs"
        url = uri("https://mvn.lumine.io/repository/maven-public/")
    }
    maven {
        name = "phoenix"
        url = uri("https://nexus.phoenixdevt.fr/repository/maven-public/")
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.nexomc:nexo:0.7.0")
    compileOnly("io.lumine:Mythic-Dist:5.8.2")
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT")

    compileOnly("redis.clients:jedis:5.2.0")
    compileOnly("com.zaxxer:HikariCP:6.3.0")
    compileOnly ("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    compileOnly("commons-lang:commons-lang:2.6")
    compileOnly("org.bstats:bstats-bukkit:3.0.2")

    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation(project(":common"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs()) {
            filesMatching("plugin.yml") {
                expand(
                    "name" to "Nexus",
                    "version" to version
                )

            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("Nexus-bukkit")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
        // relocate("org.bstats", "io.nexstudios.nexus.libs.bstats")
        relocate("co.aikar.commands", "io.nexstudios.nexus.libs.commands")
        relocate("co.aikar.locales", "io.nexstudios.nexus.libs.locales")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar.get()) {
                classifier = null
            }
            groupId = "com.github.lightPlugins"
            artifactId = "Nexus-bukkit"
            version = rootProject.version.toString()
        }
    }
}

tasks.named("publishMavenPublicationToMavenLocal") {
    dependsOn(tasks.shadowJar)
    dependsOn(tasks.jar)
}

tasks.jar {
    enabled = false
}