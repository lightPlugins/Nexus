plugins {
    id("io.papermc.paperweight.userdev") apply false
}

group = "io.nexstudios"
version = rootProject.version

subprojects {
    dependencies {
        compileOnly(project(":nexus-core"))
    }
}