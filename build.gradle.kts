plugins {
    id("java")
}

group = "io.lightstudios.nexus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

}

tasks.named<Jar>("jar") {
    enabled = false
}