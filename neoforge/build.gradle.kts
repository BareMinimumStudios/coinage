import java.util.Properties

plugins {
    id("net.neoforged.moddev")
}

val pins = rootProject.extra["targetPins"] as Properties
val crunchDependency = rootProject.extra["crunchDependency"] as String
val runDirectory = file("run/" + pins.getProperty("minecraft_version"))

neoForge {
    version = pins.getProperty("neoforge_version")
    runs {
        configureEach { gameDirectory = runDirectory }
        create("client") { client() }
        create("server") {
            server()
            programArgument("--nogui")
        }
    }
    mods { create("coinage") { sourceSet(sourceSets.main.get()) } }
}

dependencies {
    add("jarJar", dependencies.create(crunchDependency) { isTransitive = false })
    add("implementation", dependencies.create(pins.getProperty("fzzy_neoforge_coordinate")) {
        exclude(group = "dev.nyon", module = "KotlinLangForge")
    })
    add("implementation", "thedarkcolour:kotlinforforge-neoforge:" + pins.getProperty("kotlin_for_forge_version"))
}

// Plain Java libraries must also be visible to NeoForge development runs.
configurations.named("additionalRuntimeClasspath") { extendsFrom(configurations.getByName("expressionEngine")) }
