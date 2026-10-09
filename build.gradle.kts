plugins {
    java
    checkstyle
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
    "minecraft_version_range",
    "neoforge_version",
    "neoforge_version_range",
    "loader_version_range",
    "geckolib_version",
    "geckolib_version_range",
).associateWith { property(it) as String }

val modId = modProperties.getValue("mod_id")
val modVersion = modProperties.getValue("mod_version")

group = property("mod_group_id") as String
version = modVersion

base {
    archivesName = "$modId-neoforge-${modProperties.getValue("minecraft_version")}"
}

// Core modules: plain Java without Minecraft, each built and tested on its own.
// Their sources are compiled into the mod itself, so the mod ships as one jar.
val coreModules = listOf(
    ":nexus-core-api",
    ":nexus-resource-api",
    ":nexus-storage-api",
    ":nexus-energy-api",
    ":nexus-transport-api",
    ":nexus-upgrade-api",
    ":nexus-machine-api",
    ":nexus-automation-api",
    ":nexus-network-api",
    ":nexus-network",
)

val junitVersion = "5.11.3"
val assertjVersion = "3.26.3"
val jspecifyVersion = "1.0.0"
val checkstyleVersion = "10.26.1"

allprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }

    extensions.configure<CheckstyleExtension> {
        toolVersion = checkstyleVersion
        configDirectory = rootProject.file("config/checkstyle")
        maxWarnings = 0
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    group = rootProject.group
    version = rootProject.version

    dependencies {
        "api"("org.jspecify:jspecify:$jspecifyVersion")
        "testImplementation"(platform("org.junit:junit-bom:$junitVersion"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core:$assertjVersion")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

project(":nexus-storage-api") {
    dependencies {
        "api"(project(":nexus-core-api"))
        "api"(project(":nexus-resource-api"))
    }
}

project(":nexus-energy-api") {
    dependencies {
        "api"(project(":nexus-core-api"))
    }
}

project(":nexus-transport-api") {
    dependencies {
        "api"(project(":nexus-resource-api"))
    }
}

project(":nexus-machine-api") {
    dependencies {
        "api"(project(":nexus-resource-api"))
    }
}

project(":nexus-automation-api") {
    dependencies {
        "api"(project(":nexus-core-api"))
        "api"(project(":nexus-resource-api"))
    }
}

project(":nexus-network") {
    dependencies {
        "api"(project(":nexus-energy-api"))
        "api"(project(":nexus-storage-api"))
        "api"(project(":nexus-transport-api"))
        "api"(project(":nexus-machine-api"))
        "api"(project(":nexus-automation-api"))
        "api"(project(":nexus-network-api"))
        "testImplementation"(project(":nexus-network-test"))
    }
}

// Test fixtures of the core; never part of the mod.
project(":nexus-network-test") {
    dependencies {
        "api"(project(":nexus-storage-api"))
    }
}

sourceSets.main {
    coreModules.forEach { java.srcDir(project(it).file("src/main/java")) }
}

val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[gametest.compileClasspathConfigurationName].extendsFrom(configurations.compileClasspath.get())
configurations[gametest.runtimeClasspathConfigurationName].extendsFrom(configurations.runtimeClasspath.get())

neoForge {
    version = modProperties.getValue("neoforge_version")

    addModdingDependenciesTo(gametest)

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
        create("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gametest
            gameDirectory = project.file("run/gametest")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(gametest)
        }
    }
}

repositories {
    maven("https://maven.blamejared.com") {
        content { includeGroup("mezz.jei") }
    }
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        content { includeGroup("com.geckolib") }
    }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.k-4u.nl") {
        content { includeGroup("mcjty.theoneprobe") }
    }
    maven("https://maven.shedaniel.me") {
        content {
            includeGroup("me.shedaniel")
            includeGroup("me.shedaniel.cloth")
            includeGroup("dev.architectury")
        }
    }
}

dependencies {
    implementation("org.jspecify:jspecify:$jspecifyVersion")
    val geckolib = "com.geckolib:geckolib-neoforge-${property("minecraft_version")}:${property("geckolib_version")}"
    implementation(geckolib)
    "interfaceInjectionData"(geckolib)
    compileOnly("mezz.jei:jei-${property("minecraft_version")}-neoforge-api:${property("jei_version")}")
    compileOnly("me.shedaniel:RoughlyEnoughItems-api-neoforge:${property("rei_version")}")
    compileOnly("maven.modrinth:jade:${property("jade_version")}")
    compileOnly("mcjty.theoneprobe:theoneprobe:${property("top_version")}:api")
    compileOnly("maven.modrinth:curios:${property("curios_version")}")
}

tasks.processResources {
    inputs.properties(modProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(modProperties)
    }
}
