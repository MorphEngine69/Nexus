pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "nexus"

include(
    "nexus-core-api",
    "nexus-resource-api",
    "nexus-storage-api",
    "nexus-energy-api",
    "nexus-transport-api",
    "nexus-upgrade-api",
    "nexus-network-api",
    "nexus-network",
    "nexus-network-test",
)
