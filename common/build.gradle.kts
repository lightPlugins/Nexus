plugins {
    id("java")
    id("maven-publish")
    id("io.freefair.lombok") version "8.11"
}

group = "io.lightstudios.nexus.common"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
}