plugins {
    java
    id("net.neoforged.moddev") version "2.0.147"
}

val modProperties = listOf(
    "mod_id",
    "mod_name",
    "mod_version",
    "mod_license",
    "mod_authors",
    "mod_description",
    "minecraft_version",
    "neoforge_version",
    "neoforge_version_range",
    "loader_version_range",
).associateWith { property(it) as String }

val modId = modProperties.getValue("mod_id")
val modVersion = modProperties.getValue("mod_version")

group = property("mod_group_id") as String
version = modVersion

base {
    archivesName = modId
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

neoForge {
    version = modProperties.getValue("neoforge_version")

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.properties(modProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(modProperties)
    }
}
