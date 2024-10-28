import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "1.7.1"
  id("xyz.jpenilla.run-paper") version "2.3.0" // Adds runServer and runMojangMappedServer tasks for testing
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.1.1" // Generates plugin.yml based on the Gradle config
}

group = "foundation.esoteric"
version = "0.1.0"
description = "An experimental plugin designed to explore the mechanics of Minecraft servers, debug and test features."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.TheSlimySwamp:core-plugin:v0.2.2:dev-all")

    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}

bukkitPluginYaml {
    name = "TSSSlimeLabs"
    description = project.description
    authors.addAll("Esoteric Foundation", "Esoteric Enderman")

    version = project.version.toString()
    apiVersion = "1.21"
    depend.addAll("TSSCore")
    main = "foundation.esoteric.tss.minecraft.plugins.experimental.TSSSlimeLabsPlugin"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
}
