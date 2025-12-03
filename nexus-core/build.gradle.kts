group = "io.nexstudios"
version = "1.0-SNAPSHOT"

plugins {
    id("io.papermc.paperweight.userdev")
    id("io.freefair.lombok") version "8.11"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.auxilor.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.bg-software.com/repository/api/")
    maven("https://maven.devs.beer/")
    maven("https://repo.auroramc.gg/releases/")
}

dependencies {
    compileOnly(project(":nexus-nms"))
    paperweight.paperDevBundle("1.21.7-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.nexomc:nexo:0.7.0")
    compileOnly("io.lumine:Mythic-Dist:5.9.5")
    compileOnly("io.lumine:MythicLib-dist:1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT")
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.6")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14")
    compileOnly("com.bgsoftware:SuperiorSkyblockAPI:2025.1")
    compileOnly("gg.auroramc:Aurora:2.4.2")
    compileOnly("gg.auroramc:AuroraCollections:1.5.7")
    compileOnly("com.willfp:eco:6.77.2")
    compileOnly("com.willfp:EcoSkills:3.67.0")
    compileOnly("com.willfp:EcoItems:5.66.0")
    compileOnly("dev.lone:api-itemsadder:4.0.10")

    compileOnly("redis.clients:jedis:7.1.0") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    compileOnly("com.zaxxer:HikariCP:6.3.0")
    compileOnly ("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    compileOnly("commons-lang:commons-lang:2.6")

    compileOnly(fileTree("${rootDir}/libs") {
        include("*.jar")
    })


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
}