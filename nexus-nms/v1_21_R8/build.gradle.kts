plugins {
    id("io.papermc.paperweight.userdev")
}

group = "io.nexstudios"
version = rootProject.version

repositories {

}


dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.release.set(21)
    }
}