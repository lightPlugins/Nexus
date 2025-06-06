plugins {
    id("java")
    kotlin("jvm") version "1.8.0" // Ensure you have the Kotlin plugin applied
    id("io.freefair.lombok") version "8.11"
    id("com.gradleup.shadow") version "8.3.5"
    id("maven-publish")
}

group = "io.lightstudios.nexus.velocity"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }

    maven {
        name = "codemc"
        url = uri("https://repo.codemc.org/repository/maven-public")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.0")
    // implementation("org.yaml:snakeyaml:2.4")
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation ("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    implementation("commons-lang:commons-lang:2.6")
    implementation("org.bstats:bstats-velocity:3.0.2")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs()) {
            filesMatching("velocity-plugin.json") {
                expand(
                    "name" to "nexus",
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
        archiveBaseName.set("Nexus-velocity")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
        relocate("com.zaxxer.hikari", "io.lightstudios.nexus.lib.hikari")
        relocate("org.mariadb.jdbc", "io.lightstudios.nexus.lib.mariadb")
        relocate("redis.clients.jedis", "io.lightstudios.nexus.lib.jedis")
        relocate("org.bstats", "io.lightstudios.nexus.lib.bstats")
        //relocate("org.yaml.snakeyaml", "io.lightstudios.proxy.util.libs.snakeyaml")
        relocate("org.sqlite", "io.lightstudios.nexus.lib.sqlite")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar.get()) {
                classifier = null
            }
            groupId = "com.github.lightPlugins"
            artifactId = "Nexus-velocity"
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