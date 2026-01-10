import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runwaterfall.task.RunWaterfall
import xyz.jpenilla.runvelocity.task.RunVelocity




val PLUGIN_NAME = "OriginBlacklist"
val PLUGIN_IDEN = "originblacklist"
val PLUGIN_DOMN = "xyz.webmc"
val PLUGIN_DESC = "An eaglercraft client blacklist plugin."
val PLUGIN_VERS = "2.0.0"
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




val PLUGIN_DEPA_J = getJSONObj(PLUGIN_DEPA)
val PLUGIN_DEPB_J = getJSONObj(PLUGIN_DEPB)
val PLUGIN_DEPC_J = getJSONObjMerge(PLUGIN_DEPC, PLUGIN_SDPC)
val PLUGIN_SDPA_J = getJSONObj(PLUGIN_SDPA)
val PLUGIN_SDPB_J = getJSONObj(PLUGIN_SDPB)
val PLUGIN_PROV_J = getJSONObj(PLUGIN_PROV)
val PLUGIN_ATHR_J = getJSONObj(PLUGIN_ATHR)
val PLUGIN_CTBR_J = getJSONObj(PLUGIN_CTBR)

plugins {
  id("java")
  id("com.gradleup.shadow") version "9.3.0"
  id("xyz.jpenilla.run-paper") version "3.0.2"
  id("xyz.jpenilla.run-waterfall") version "3.0.2"
  id("xyz.jpenilla.run-velocity") version "3.0.2"
}

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

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release.set(17)
}

tasks.withType<ProcessResources>() {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  outputs.upToDateWhen { false }
  doFirst {
    filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
			expand(mapOf(
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
			))
		}
  }

  inputs.files(tasks.named<JavaCompile>("compileJava").map { it.outputs.files })
}

tasks.withType<RunServer>() {
  minecraftVersion("1.12.2")
  runDirectory.set(layout.projectDirectory.dir("run/paper"))
  downloadPlugins {
    github("lax1dude", "eaglerxserver", "v1.0.8", "EaglerXServer.jar")
    modrinth("placeholderapi", "2.11.7")
  }
}

tasks.withType<RunWaterfall>() {
  waterfallVersion("1.21")
  runDirectory.set(layout.projectDirectory.dir("run/waterfall"))
  downloadPlugins {
    github("lax1dude", "eaglerxserver", "v1.0.8", "EaglerXServer.jar")
  }
}

tasks.withType<RunVelocity>() {
  velocityVersion("3.4.0-SNAPSHOT")
  runDirectory.set(layout.projectDirectory.dir("run/velocity"))
  downloadPlugins {
    github("lax1dude", "eaglerxserver", "v1.0.8", "EaglerXServer.jar")
    modrinth("miniplaceholders", "3.1.0")
  }
}

tasks.jar {
  archiveFileName.set("$PLUGIN_NAME-$PLUGIN_VERS.jar")
}

tasks.shadowJar {
  relocate("org.bstats", "$PLUGIN_DOMN.$PLUGIN_IDEN.shaded.bstats")
  relocate("de.marhali.json5", "$PLUGIN_DOMN.$PLUGIN_IDEN.shaded.json5")
  relocate("org.semver4j.semver4j", "$PLUGIN_DOMN.$PLUGIN_IDEN.shaded.semver4j")
  // relocate("net.kyori.adventure", "$PLUGIN_DOMN.$PLUGIN_IDEN.shaded.adventure")
  archiveFileName.set("$PLUGIN_NAME-$PLUGIN_VERS.jar")
}

tasks.register("printVars") {
  group = "help"
  doLast {
    println("VERS = " + PLUGIN_VERS)
    println("AFCT = " + tasks.named("shadowJar").get().outputs.files.singleFile.name)
  }
}

fun getJSONObj(list: List<String>): String {
  return if (list.isNotEmpty()) {
    list.joinToString(
      separator = "\", \"",
      prefix = "\"",
      postfix = "\""
    )
  } else {
    ""
  }
}

fun getJSONObjMerge(a: List<String>, b: List<String>): String {
  val c = a.joinToString(", ") { "{\"id\":\"$it\",\"optional\":false}" }
  val d = b.joinToString(", ") { "{\"id\":\"$it\",\"optional\":true}" }
  return listOf(c, d).filter { it.isNotEmpty() }.joinToString(",")
}
