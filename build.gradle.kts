import java.util.Properties
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.FileTreeElement
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") apply false
    id("net.fabricmc.fabric-loom-remap") version "1.17.21" apply false
    id("net.fabricmc.fabric-loom") version "1.17.21" apply false
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("dev.mixinmcp.decompile") version "1.5.0" apply false
}

val selectedTarget = providers.gradleProperty("coinageTarget").getOrElse("1.21.1")
val profileFile = file("targets/$selectedTarget.properties")
if (!profileFile.isFile) throw GradleException("Unknown Minecraft target $selectedTarget")
val pins = Properties().apply { profileFile.reader(Charsets.UTF_8).use { load(it) } }
val modernTarget = selectedTarget == "26.3"
val targetJava = pins.getProperty("java_version").toInt()
val crunchDependency = "com.github.boxbeam:Crunch:0c7d52fc19"

extra["targetPins"] = pins
extra["modernTarget"] = modernTarget
extra["crunchDependency"] = crunchDependency

fun isReplaced(roots: List<File>, entry: FileTreeElement): Boolean =
    roots.withIndex().any { (index, root) ->
        entry.file.toPath().startsWith(root.toPath()) &&
            roots.drop(index + 1).any { it.resolve(entry.relativePath.pathString).isFile }
    }

allprojects {
    group = "net.bms"
    version = pins.getProperty("mod_version")
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("$selectedTarget/$name"))
    repositories {
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeModule("com.github.boxbeam", "Crunch") }
        }
        maven("https://api.modrinth.com/maven")
        maven("https://maven.fzzyhmstrs.me/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://thedarkcolour.github.io/KotlinForForge/") {
            content { includeGroup("thedarkcolour") }
        }
    }
    configurations.configureEach { exclude(group = "com.terraformersmc", module = "modmenu") }
    repositories.withType<MavenArtifactRepository>().configureEach {
        // Loom generates remapped artifacts locally.
        if (url.scheme in setOf("http", "https")) {
            content { excludeGroupByRegex("remapped[.].*") }
        }
    }
    extensions.configure<BasePluginExtension> { archivesName.set("coinage-" + project.name) }
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJava))
        withSourcesJar()
    }
    val kotlinExtension = extensions.getByType<KotlinJvmProjectExtension>()
    kotlinExtension.jvmToolchain(targetJava)

    val kotlinRoots = mutableListOf(rootProject.file("src/main/kotlin"), file("src/main/kotlin"))
    val javaRoots = mutableListOf(rootProject.file("src/main/java"), file("src/main/java"))
    val resourceRoots = mutableListOf(rootProject.file("src/main/resources"), file("src/main/resources"))
    val testRoots = mutableListOf(rootProject.file("src/test/kotlin"))
    if (modernTarget) {
        kotlinRoots += listOf(rootProject.file("src/26.3/kotlin"), file("src/26.3/kotlin"))
        javaRoots += listOf(rootProject.file("src/26.3/java"), file("src/26.3/java"))
        resourceRoots += listOf(rootProject.file("src/26.3/resources"), file("src/26.3/resources"))
        testRoots += rootProject.file("src/26.3/test/kotlin")
    } else {
        javaRoots += rootProject.file("src/1.21.1/java")
    }

    val main = extensions.getByType<SourceSetContainer>().named("main")
    main.configure {
        java.setSrcDirs(javaRoots)
        java.exclude { isReplaced(javaRoots, it) }
        resources.setSrcDirs(resourceRoots)
        resources.exclude { isReplaced(resourceRoots, it) }
    }
    kotlinExtension.sourceSets.named("main") {
        kotlin.setSrcDirs(kotlinRoots)
        kotlin.exclude { isReplaced(kotlinRoots, it) }
    }
    if (name == "fabric") {
        kotlinExtension.sourceSets.named("test") {
            kotlin.setSrcDirs(testRoots)
            kotlin.exclude { isReplaced(testRoots, it) }
        }
    }
    tasks.named<KotlinCompile>("compileKotlin") { source(main.map { it.java }) }
    val sourceRoots = kotlinRoots + javaRoots
    tasks.named<Jar>("sourcesJar") { exclude { isReplaced(sourceRoots, it) } }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(targetJava)
        options.encoding = "UTF-8"
    }

    val expressionEngine = configurations.create("expressionEngine") { isTransitive = false }
    configurations.named("implementation") { extendsFrom(expressionEngine) }
    dependencies {
        add(expressionEngine.name, crunchDependency)
        add("testImplementation", platform("org.junit:junit-bom:5.14.0"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
        val values = mapOf(
            "version" to project.version,
            "minecraftVersion" to pins.getProperty("minecraft_version"),
            "javaVersion" to pins.getProperty("java_version"),
            "javaCompatibility" to "JAVA_$targetJava",
            "kotlinLoaderVersion" to pins.getProperty("kotlin_loader_range"),
            "neoForgeVersion" to pins.getProperty("neoforge_version"),
            "fabricKotlinVersion" to pins.getProperty("fabric_kotlin_version")
        )
        inputs.properties(values)
        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "coinage.mixins.json", "coinage.fabric.mixins.json")) {
            expand(values)
        }
    }
    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE"), rootProject.file("LICENSE-UPSTREAM"), rootProject.file("NOTICE.md"))
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

tasks.named("build") { dependsOn(subprojects.map { it.path + ":build" }) }
tasks.named("clean") { dependsOn(subprojects.map { it.path + ":clean" }) }
