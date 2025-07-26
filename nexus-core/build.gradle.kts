group = "io.nexstudios"
version = "1.0-SNAPSHOT"

plugins {
    id("io.papermc.paperweight.userdev")
    id("io.freefair.lombok") version "8.11"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
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

    compileOnly("redis.clients:jedis:5.2.0")
    compileOnly("com.zaxxer:HikariCP:6.3.0")
    compileOnly ("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    compileOnly("commons-lang:commons-lang:2.6")

    compileOnly("com.willfp:EcoItems:5.63.1")
    compileOnly("com.willfp:eco:6.76.2")

    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
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