import java.util.Properties
import net.fabricmc.loom.api.LoomGradleExtensionAPI

val pins = rootProject.extra["targetPins"] as Properties
val modernTarget = rootProject.extra["modernTarget"] as Boolean
val crunchDependency = rootProject.extra["crunchDependency"] as String

apply(plugin = if (modernTarget) "net.fabricmc.fabric-loom" else "net.fabricmc.fabric-loom-remap")
apply(plugin = "dev.mixinmcp.decompile")

val loom = extensions.getByType<LoomGradleExtensionAPI>()
val minecraftVersion = pins.getProperty("minecraft_version")
val modDependencies = if (modernTarget) "implementation" else "modImplementation"

dependencies {
    add("include", dependencies.create(crunchDependency) {
        isTransitive = false
    })
    add("minecraft", "com.mojang:minecraft:$minecraftVersion")
    if (!modernTarget) add("mappings", loom.officialMojangMappings())
    add(modDependencies, "net.fabricmc:fabric-loader:" + pins.getProperty("loader_version"))
    add(modDependencies, "net.fabricmc.fabric-api:fabric-api:" + pins.getProperty("fabric_api_version"))
    add(modDependencies, "net.fabricmc:fabric-language-kotlin:" + pins.getProperty("fabric_kotlin_version"))
    add(modDependencies, "me.fzzyhmstrs:fzzy_config:" + pins.getProperty("fzzy_config_version"))
}

loom.runs.configureEach { runDir = "run/$minecraftVersion" }
