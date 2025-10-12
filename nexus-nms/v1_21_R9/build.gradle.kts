plugins {
    id("io.papermc.paperweight.userdev")
}

group = "io.nexstudios"
version = rootProject.version

dependencies {
    paperweight.paperDevBundle("1.21.9-R0.1-SNAPSHOT")

    implementation("net.kyori:adventure-text-minimessage:4.25.0") {
        version {
            strictly("4.25.0")
        }
        exclude(group = "net.kyori", module = "adventure-api")
    }
}