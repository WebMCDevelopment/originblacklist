plugins {
  id("java")
  id("eclipse")
  id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
  id("com.gradleup.shadow") version "9.3.0"
  id("xyz.jpenilla.run-velocity") version "2.3.1"
}

group = "dev.colbster937"
version = "1.1.3"
description = "A reimplementation of OriginBlacklist for EaglerXServer"

val targetJavaVersion = 17

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://oss.sonatype.org/content/groups/public/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
  maven("https://repo.md-5.net/content/repositories/releases/")
  maven("https://repo.aikar.co/nexus/content/groups/aikar/")
  maven("https://repo.lax1dude.net/repository/releases/")
}

dependencies {
  compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
  compileOnly("org.bukkit:bukkit:1.8-R0.1-SNAPSHOT")
  compileOnly("net.md-5:bungeecord-api:1.8-SNAPSHOT")
  compileOnly("net.lax1dude.eaglercraft.backend:api-velocity:1.0.0")
  compileOnly("net.lax1dude.eaglercraft.backend:api-bungee:1.0.0")
  compileOnly("net.lax1dude.eaglercraft.backend:api-bukkit:1.0.0")
  implementation("org.yaml:snakeyaml:2.2")
  implementation("net.kyori:adventure-text-serializer-legacy:4.20.0")
  implementation("net.kyori:adventure-text-minimessage:4.20.0")
  implementation("com.github.seancfoley:ipaddress:5.3.4")
  annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks {
  named<xyz.jpenilla.runvelocity.task.RunVelocity>("runVelocity") {
    velocityVersion("3.4.0-SNAPSHOT")
  }
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
}

tasks.processResources {
  filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json", "Base.java")) {
    expand(
      mapOf(
        "version" to project.version,
        "description" to project.description
      )
    )
  }
}

tasks.shadowJar {
  relocate("org.yaml.snakeyaml", "dev.colbster937.shaded.snakeyaml")
  relocate("inet.ipaddr", "dev.colbster937.shaded.ipaddr")
  archiveVersion.set("")
  archiveClassifier.set("")
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release.set(targetJavaVersion)
}
