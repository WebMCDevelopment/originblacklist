import groovy.lang.Closure
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.palantir.gradle.gitversion.VersionDetails
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runwaterfall.task.RunWaterfall
import xyz.jpenilla.runvelocity.task.RunVelocity




val PLUGIN_NAME = "OriginBlacklist"
val PLUGIN_IDEN = "originblacklist"
val PLUGIN_DOMN = "xyz.webmc.$PLUGIN_IDEN"
val PLUGIN_DESC = "An eaglercraft client blacklist plugin."
val PLUGIN_VERS = "2.0.8"
val PLUGIN_SITE = "https://github.com/WebMCDevelopment/$PLUGIN_IDEN"
val PLUGIN_DEPA = listOf("EaglercraftXServer")
val PLUGIN_DEPB = listOf("EaglercraftXServer")
val PLUGIN_DEPC = listOf("eaglerxserver")
val PLUGIN_SDPA = listOf("PlaceholderAPI")
val PLUGIN_SDPB = listOf("PAPIProxyBridge")
val PLUGIN_SDPC = listOf("papiproxybridge")
val PLUGIN_PROV = emptyList<String>()
val PLUGIN_ATHR = listOf("Colbster937")
val PLUGIN_CTBR = emptyList<String>()




val PLUGIN_DEPA_J = getBukkitBungeeDeps(PLUGIN_DEPA)
val PLUGIN_DEPB_J = getBukkitBungeeDeps(PLUGIN_DEPB)
val PLUGIN_DEPC_J = getVelocityDeps(PLUGIN_DEPC, PLUGIN_SDPC)
val PLUGIN_SDPA_J = getBukkitBungeeDeps(PLUGIN_SDPA)
val PLUGIN_SDPB_J = getBukkitBungeeDeps(PLUGIN_SDPB)
val PLUGIN_PROV_J = getBukkitBungeeDeps(PLUGIN_PROV)
val PLUGIN_ATHR_J = getBukkitBungeeDeps(PLUGIN_ATHR)
val PLUGIN_CTBR_J = getBukkitBungeeDeps(PLUGIN_CTBR)

val EAGXS_VER = "1.0.8"

plugins {
  id("java")
  id("com.gradleup.shadow") version "9.3.0"
  id("com.palantir.git-version") version "4.3.0"
  id("xyz.jpenilla.run-paper") version "3.0.2"
  id("xyz.jpenilla.run-waterfall") version "3.0.2"
  id("xyz.jpenilla.run-velocity") version "3.0.2"
}

@Suppress("UNCHECKED_CAST")
val GIT_INFO = (extra["versionDetails"] as Closure<VersionDetails>)()

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://oss.sonatype.org/content/groups/public/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
  maven("https://repo.md-5.net/content/repositories/releases/")
  maven("https://repo.aikar.co/nexus/content/groups/aikar/")
  maven("https://repo.lax1dude.net/repository/releases/")
  maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
  maven("https://repo.william278.net/releases/")
}

dependencies {
  compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
  compileOnly("org.bukkit:bukkit:1.8-R0.1-SNAPSHOT")
  compileOnly("net.md-5:bungeecord-api:1.21-R0.5-SNAPSHOT")
  compileOnly("net.lax1dude.eaglercraft.backend:api-velocity:1.0.0")
  compileOnly("net.lax1dude.eaglercraft.backend:api-bungee:1.0.0")
  compileOnly("net.lax1dude.eaglercraft.backend:api-bukkit:1.0.0")
  compileOnly("me.clip:placeholderapi:2.11.7")
  compileOnly("net.william278:papiproxybridge:1.8.4")
  implementation("org.semver4j:semver4j:6.0.0")
  implementation("de.marhali:json5-java:3.0.0")
  implementation("net.kyori:adventure-text-minimessage:4.26.1")
  implementation("net.kyori:adventure-text-serializer-legacy:4.26.1")
  implementation("com.github.seancfoley:ipaddress:5.5.1")
  implementation("org.bstats:bstats-bukkit:3.1.0")
  implementation("org.bstats:bstats-bungeecord:3.1.0")
  implementation("org.bstats:bstats-velocity:3.1.0")
}

sourceSets {
	named("main") {
		java.srcDir("./src/main/java")
		resources.srcDir("./src/main/resources")
	}
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val BUILD_PROPS = mapOf(
  "plugin_name" to PLUGIN_NAME,
  "plugin_iden" to PLUGIN_IDEN,
  "plugin_desc" to PLUGIN_DESC,
  "plugin_vers" to PLUGIN_VERS,
  "plugin_site" to PLUGIN_SITE,
  "plugin_depa" to PLUGIN_DEPA_J,
  "plugin_depb" to PLUGIN_DEPB_J,
  "plugin_depc" to PLUGIN_DEPC_J,
  "plugin_sdpa" to PLUGIN_SDPA_J,
  "plugin_sdpb" to PLUGIN_SDPB_J,
  "plugin_prov" to PLUGIN_PROV_J,
  "plugin_athr" to PLUGIN_ATHR_J,
  "plugin_ctbr" to PLUGIN_CTBR_J,
  "git_cm_hash" to GIT_INFO.gitHashFull,
)

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release.set(17)
}

tasks.withType<ProcessResources>().configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  outputs.upToDateWhen { false }
  
  doFirst {
    filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
			expand(BUILD_PROPS)
		}
  }

  doLast {
    val file = destinationDir.resolve("build.properties")
    file.parentFile.mkdirs()
    file.writeText(
      BUILD_PROPS.entries.joinToString("\n") { (k, v) ->
        "$k = $v"
      }
    )
  }

  inputs.files(tasks.named<JavaCompile>("compileJava").map { it.outputs.files })
}

tasks.withType<Jar>().configureEach {
  if (this !is ShadowJar) enabled = false
}

tasks.withType<ShadowJar>().configureEach {
  doFirst {
    delete(layout.buildDirectory.dir("libs"))
    mkdir(layout.buildDirectory.dir("libs"))
  }
  relocate("inet.ipaddr", "$PLUGIN_DOMN.shaded.ipaddress")
  relocate("de.marhali.json5", "$PLUGIN_DOMN.shaded.json5")
  relocate("org.bstats", "$PLUGIN_DOMN.shaded.bstats")
  relocate("org.semver4j.semver4j", "$PLUGIN_DOMN.shaded.semver4j")
  // relocate("net.kyori.adventure", "$PLUGIN_DOMN.shaded.adventure")
  archiveFileName.set("$PLUGIN_NAME-$PLUGIN_VERS.jar")
}

tasks.named("build") {
  dependsOn(tasks.named("shadowJar"))
}

tasks.register("printVars") {
  group = "help"
  doLast {
    println("VERS = " + PLUGIN_VERS)
    println("AFCT = " + tasks.named("shadowJar").get().outputs.files.singleFile.name.removeSuffix(".jar"))
  }
}

tasks.withType<RunServer>().configureEach {
  minecraftVersion("1.12.2")
  runDirectory.set(layout.projectDirectory.dir("run/paper"))
  jvmArgs("-Dcom.mojang.eula.agree=true")
  downloadPlugins {
    github("lax1dude", "eaglerxserver", "v" + EAGXS_VER, "EaglerXServer.jar")
    modrinth("placeholderapi", "2.11.7")
  }
}

tasks.withType<RunWaterfall>().configureEach {
  waterfallVersion("1.21")
  runDirectory.set(layout.projectDirectory.dir("run/waterfall"))
  downloadPlugins {
    github("lax1dude", "eaglerxserver", "v" + EAGXS_VER, "EaglerXServer.jar")
  }
}

tasks.withType<RunVelocity>().configureEach {
  velocityVersion("3.4.0-SNAPSHOT")
  runDirectory.set(layout.projectDirectory.dir("run/velocity"))
  downloadPlugins {
    github("lax1dude", "eaglerxserver", "v" + EAGXS_VER, "EaglerXServer.jar")
    modrinth("miniplaceholders", "3.1.0")
  }
}

fun getBukkitBungeeDeps(list: List<String>): String {
  return list.joinToString(
    prefix = "[",
    postfix = "]"
  ) { "\"$it\"" }
}

fun getVelocityDeps(a: List<String>, b: List<String>): String {
  val c = a.joinToString(", ") { "{\"id\":\"$it\",\"optional\":false}" }
  val d = b.joinToString(", ") { "{\"id\":\"$it\",\"optional\":true}" }
  return "[" + listOf(c, d).filter { it.isNotEmpty() }.joinToString(",") + "]"
}
