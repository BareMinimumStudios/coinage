pluginManagement {
    val target = providers.gradleProperty("coinageTarget").getOrElse("1.21.1")
    val profile = settingsDir.resolve("targets/$target.properties")
    if (!profile.isFile) throw GradleException("Unknown Minecraft target $target")
    val pins = java.util.Properties()
    profile.reader(Charsets.UTF_8).use { pins.load(it) }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.jvm") useVersion(pins.getProperty("kotlin_version"))
        }
    }
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.muon.rip/releases")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "coinage"
when (val loader = providers.gradleProperty("coinageLoader").getOrElse("both")) {
    "both" -> include("fabric", "neoforge")
    "fabric", "neoforge" -> include(loader)
    else -> throw GradleException("Unknown Coinage loader $loader")
}
