package xyz.webmc.originblacklist.base;

import xyz.webmc.originblacklist.base.config.OriginBlacklistConfig;
import xyz.webmc.originblacklist.base.enums.EnumBlacklistType;
import xyz.webmc.originblacklist.base.enums.EnumConnectionType;
import xyz.webmc.originblacklist.base.enums.EnumLogLevel;
import xyz.webmc.originblacklist.base.events.OriginBlacklistLoginEvent;
import xyz.webmc.originblacklist.base.events.OriginBlacklistMOTDEvent;
import xyz.webmc.originblacklist.base.http.OriginBlacklistRequestHandler;
import xyz.webmc.originblacklist.base.metrics.GenericMetricsAdapter;
import xyz.webmc.originblacklist.base.util.BuildInfo;
import xyz.webmc.originblacklist.base.util.IOriginBlacklistPlugin;
import xyz.webmc.originblacklist.base.util.OPlayer;
import xyz.webmc.originblacklist.base.util.UpdateChecker;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.IBasePlayer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPlayer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.query.IMOTDConnection;
import org.bstats.charts.AdvancedPie;
import org.semver4j.Semver;

@SuppressWarnings({ "rawtypes" })
public final class OriginBlacklist {
  private static final String COMMIT_L = BuildInfo.get("git_cm_hash");
  private static final String COMMIT_S = COMMIT_L.substring(0, 8);

  public static final Semver REQUIRED_API_VER = new Semver("1.0.2");
  public static final String GENERIC_STR = "GENERIC";
  public static final String UNKNOWN_STR = "UNKNOWN";
  public static final String CENSORED_STR = "CENSORED";
  public static final String PLUGIN_REPO = "WebMCDevelopment/originblacklist";

  private final IOriginBlacklistPlugin plugin;
  private final OriginBlacklistConfig config;
  private final GenericMetricsAdapter metrics;
  private final Json5 json5;
  private String updateURL;
  private Path jarFile;

  public OriginBlacklist(final IOriginBlacklistPlugin plugin) {
    this.plugin = plugin;
    this.config = new OriginBlacklistConfig(this);
    this.metrics = plugin.getMetrics();
    this.json5 = Json5.builder(builder -> builder.prettyPrinting().indentFactor(0).build());
  }

  public final void init() {
    this.jarFile = this.plugin.getPluginJarPath();
    this.plugin.scheduleRepeat(() -> {
      this.checkForUpdates();
    }, this.config.getInteger("update_checker.check_timer"), TimeUnit.SECONDS);
    if (this.isBlacklistAPIEnabled()) {
      OriginBlacklistRequestHandler.register(this);
    }
    this.metrics.addCustomChart(new AdvancedPie("player_types", () -> {
      final Map<String, Integer> playerMap = new HashMap<>();
      for (final Object player : this.getEaglerAPI().getAllPlayers()) {
        if (player instanceof IBasePlayer bPlayer) {
          final String key = (bPlayer instanceof IEaglerPlayer) ? "Eagler" : "Java";
          playerMap.put(key, playerMap.getOrDefault(key, 0) + 1);
        }
      }
      return playerMap;
    }));
    this.plugin.log(EnumLogLevel.INFO, "Initialized Plugin");
    this.plugin.log(EnumLogLevel.DEBUG, "Commit " + COMMIT_L);
    if (this.isMetricsEnabled()) {
      this.metrics.start();
    }
  }

  public final void shutdown() {
    this.plugin.log(EnumLogLevel.INFO, "Shutting down...");
    if (this.isBlacklistAPIEnabled()) {
      OriginBlacklistRequestHandler.unRegister(this);
    }
    if (this.isMetricsEnabled()) {
      this.metrics.shutdown();
    }
    this.plugin.shutdown();
  }

  public final void handleReload() {
    try {
      if (this.isBlacklistAPIEnabled()) {
        OriginBlacklistRequestHandler.register(this);
      } else {
        OriginBlacklistRequestHandler.unRegister(this);
      }
    } catch (final Throwable t) {
    }
    try {
      if (this.isMetricsEnabled()) {
        this.metrics.start();
      } else {
        this.metrics.shutdown();
      }
    } catch (final Throwable t) {
    }
  }

  public final void handleLogin(final OriginBlacklistLoginEvent event) {
    final OPlayer player = event.getPlayer();
    final EnumBlacklistType blacklisted = this.testBlacklist(player);
    if (blacklisted != EnumBlacklistType.NONE) {
      final String blacklisted_value;
      if (blacklisted == EnumBlacklistType.ORIGIN) {
        blacklisted_value = player.getOrigin();
      } else if (blacklisted == EnumBlacklistType.BRAND) {
        blacklisted_value = player.getBrand();
      } else if (blacklisted == EnumBlacklistType.NAME) {
        blacklisted_value = player.getName();
      } else if (blacklisted == EnumBlacklistType.ADDR) {
        blacklisted_value = player.getAddr();
      } else {
        blacklisted_value = UNKNOWN_STR;
      }
      this.plugin.kickPlayer(this.getBlacklistedComponent("kick", blacklisted.getArrayString(),
          blacklisted.getAltString(), blacklisted.getString(), "not allowed", "not allowed on the server",
          blacklisted_value, blacklisted.getActionString(), false), event);
      this.sendWebhooks(event, blacklisted);
      final String name = player.getName();
      if (isNonNullStr(name)) {
        this.plugin.log(EnumLogLevel.INFO, "Prevented blacklisted player " + name + " from joining.");
        this.updateLogFile(event, blacklisted);
      }
    }
  }

  public final void handleMOTD(final OriginBlacklistMOTDEvent event) {
    final OPlayer player = event.getPlayer();
    final EnumBlacklistType blacklisted = this.testBlacklist(player);
    if (blacklisted != EnumBlacklistType.NONE) {
      final String blacklisted_value;
      if (blacklisted == EnumBlacklistType.ORIGIN) {
        blacklisted_value = player.getOrigin();
      } else if (blacklisted == EnumBlacklistType.ADDR) {
        blacklisted_value = player.getAddr();
      } else {
        blacklisted_value = UNKNOWN_STR;
      }
      if (this.isMOTDEnabled()) {
        this.plugin
            .setMOTD(this.getBlacklistedComponent("motd", blacklisted.getArrayString(), blacklisted.getAltString(),
                blacklisted.getString(), "blacklisted", "blacklisted from the server", blacklisted_value,
                blacklisted.getActionString(), true), event);
      }
    }
  }

  public final boolean isDebugEnabled() {
    return this.config.getBoolean("debug");
  }

  public final boolean isMetricsEnabled() {
    return this.config.getBoolean("bStats");
  }

  public final boolean isMOTDEnabled() {
    return this.config.getBoolean("motd.enabled");
  }

  public final boolean isWebhooksEnabled() {
    return this.config.getBoolean("discord.webhook.enabled");
  }

  public final boolean isLogFileEnabled() {
    return this.config.getBoolean("logFile");
  }

  public final boolean isBlacklistAPIEnabled() {
    return this.config.getBoolean("blacklist_http_api");
  }

  public final OriginBlacklistConfig getConfig() {
    return this.config;
  }

  public final void setEaglerMOTD(final Component comp, final OriginBlacklistMOTDEvent event) {
    final IMOTDConnection conn = event.getEaglerEvent().getMOTDConnection();
    final List<String> lst = new ArrayList<>();
    for (final String ln : getComponentString(comp).split("\n")) {
      lst.add(ln);
    }
    final List<String> pLst = new ArrayList<>();
    for (final Json5Element ln : this.config.getArray("motd.players.hover").getAsJson5Array()) {
      pLst.add(getLegacyFromMiniMessage(
          ln.getAsString().replaceAll("%discord_invite%", this.config.getString("discord.invite"))));
    }
    conn.setServerMOTD(lst);
    conn.setPlayerTotal(this.config.getInteger("motd.players.online"));
    conn.setPlayerMax(this.config.getInteger("motd.players.max"));
    conn.setPlayerList(pLst);
    conn.setServerIcon(this.config.getIconBytes());
    conn.sendToUser();
    conn.disconnect();
  }

  public final void checkForUpdates(final Runnable action1, final Runnable action2) {
    if (this.config.getBoolean("update_checker.enabled")) {
      this.plugin.runAsync(() -> {
        this.updateURL = UpdateChecker.checkForUpdates(PLUGIN_REPO, this.plugin.getPluginVersion(),
            this.config.getBoolean("update_checker.allow_snapshots"));
        if (isNonNullStr((this.updateURL))) {
          action1.run();
          return;
        }
        action2.run();
      });
    } else {
      action2.run();
    }
  }

  public final void updatePlugin(final Runnable action1, final Runnable action2) {
    try {
      final URL url = (new URI(this.updateURL)).toURL();
      final Path jar = this.jarFile;
      final Path bak = jar.resolveSibling(jar.getFileName().toString() + ".bak");
      final Path upd = jar
          .resolveSibling(Paths.get(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8)).getFileName());

      try {
        Files.copy(jar, bak, StandardCopyOption.REPLACE_EXISTING);
      } catch (final Throwable t) {
        t.printStackTrace();
      }

      try {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", OriginBlacklist.getUserAgent());
        conn.connect();
        try (final InputStream in = conn.getInputStream()) {
          Files.copy(in, upd, StandardCopyOption.REPLACE_EXISTING);
        } finally {
          conn.disconnect();
        }
        if (Files.exists(upd)) {
          Files.delete(jar);
          Files.delete(bak);
          this.jarFile = upd;
          action1.run();
          return;
        }
      } catch (final Throwable t) {
        Files.move(bak, jar, StandardCopyOption.REPLACE_EXISTING);
        throw t;
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
    action2.run();
  }

  public final void updatePlugin() {
    this.updatePlugin(() -> {
    }, () -> {
    });
  }

  public final EnumBlacklistType testBlacklist(final OPlayer player) {
    final String name = player.getName();
    final String addr = player.getAddr();
    final String origin = player.getOrigin();
    final String brand = player.getBrand();

    final boolean whitelist = this.config.getBoolean("blacklist_to_whitelist");
    EnumBlacklistType type = EnumBlacklistType.NONE;

    if (isNonNullStr(origin)) {
      if (whitelist && !type.isBlacklisted())
        type = EnumBlacklistType.ORIGIN;
      for (final Json5Element element : this.config.getArray("blacklist.origins").getAsJson5Array()) {
        if (origin.matches(element.getAsString())) {
          if (whitelist)
            type = EnumBlacklistType.NONE;
          else if (!type.isBlacklisted())
            type = EnumBlacklistType.ORIGIN;
          break;
        }
      }
    } else if (this.config.getBoolean("block_undefined_origin")) {
      return whitelist ? EnumBlacklistType.NONE : EnumBlacklistType.ORIGIN;
    }

    if (isNonNullStr(brand)) {
      if (whitelist && !type.isBlacklisted())
        type = EnumBlacklistType.BRAND;
      for (final Json5Element element : this.config.getArray("blacklist.brands")) {
        if (brand.matches(element.getAsString())) {
          if (whitelist)
            type = EnumBlacklistType.NONE;
          else if (!type.isBlacklisted())
            type = EnumBlacklistType.BRAND;
          break;
        }
      }
    }

    if (isNonNullStr(name)) {
      if (whitelist && !type.isBlacklisted())
        type = EnumBlacklistType.NAME;
      for (final Json5Element element : this.config.getArray("blacklist.player_names")) {
        if (name.matches(element.getAsString())) {
          if (whitelist)
            type = EnumBlacklistType.NONE;
          else if (!type.isBlacklisted())
            type = EnumBlacklistType.NAME;
          break;
        }
      }
    }

    if (isNonNullStr(addr)) {
      if (whitelist && !type.isBlacklisted())
        type = EnumBlacklistType.ADDR;
      for (final Json5Element element : this.config.getArray("blacklist.ip_addresses")) {
        try {
          if ((new IPAddressString(element.getAsString()).toAddress())
              .contains((new IPAddressString(addr)).toAddress())) {
            if (whitelist)
              type = EnumBlacklistType.NONE;
            else if (!type.isBlacklisted())
              type = EnumBlacklistType.ADDR;
            break;
          }
        } catch (final AddressStringException exception) {
          if (this.isDebugEnabled())
            exception.printStackTrace();
        }
      }
    }

    return type;
  }

  public final String getBlacklistShare() {
    try {
      final Json5Object obj = new Json5Object();
      obj.addProperty("plugin_version", this.plugin.getPluginVersion().getVersion());
      obj.addProperty("git_commit", COMMIT_L);
      obj.addProperty("blacklist_to_whitelist", this.config.getBoolean("blacklist_to_whitelist"));
      obj.addProperty("block_undefined_origin", this.config.getBoolean("block_undefined_origin"));
      final Json5Object bObj = new Json5Object();
      final String[] types = new String[] { "origins", "brands", "player_names", "ip_addresses" };
      for (final String type : types) {
        bObj.add(type, this.config.getArray("blacklist." + type));
      }
      obj.add("blacklist", bObj);
      return this.json5.serialize(obj);
    } catch (final Throwable t) {
      return null;
    }
  }

  public final String getDataDir() {
    return "plugins/" + plugin.getPluginId();
  }

  public final IEaglerXServerAPI getEaglerAPI() {
    return this.plugin.getEaglerAPI();
  }

  private final Component getBlacklistedComponent(final String type, final String id, final String blockType,
      final String blockTypeAlt, final String notAllowed, final String notAllowedAlt, final String blockValue,
      final String action, final boolean isMOTD) {
    final Json5Array arr = this.config.getArray(isMOTD ? (type + ".text") : ("messages." + type));
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < arr.size(); i++) {
      if (i > 0)
        sb.append("\n");
      sb.append(arr.get(i).getAsString());
    }
    final String str = sb.toString()
        .replaceAll("%action%", this.config.getString("messages.actions." + action))
        .replaceAll("%block_type%", blockType)
        .replaceAll("%block_type%", blockType)
        .replaceAll("%not_allowed%", notAllowed)
        .replaceAll("%not_allowed_alt%", notAllowedAlt)
        .replaceAll("%blocked_value%", blockValue)
        .replaceAll("%discord_invite%", this.config.getString("discord.invite"));
    return MiniMessage.miniMessage().deserialize(str);
  }

  private final void sendWebhooks(final OriginBlacklistLoginEvent event, final EnumBlacklistType type) {
    if (this.isWebhooksEnabled()) {
      final OPlayer player = event.getPlayer();
      final EnumConnectionType connType = event.getConnectionType();
      /*
       * final String userAgent;
       * if (connType == EnumConnectionType.EAGLER) {
       * final IEaglerLoginConnection loginConn =
       * event.getEaglerEvent().getLoginConnection();
       * userAgent =
       * loginConn.getWebSocketHeader(EnumWebSocketHeader.HEADER_USER_AGENT);
       * } else {
       * userAgent = UNKNOWN_STR;
       * }
       */
      final byte[] payload = String.format(
          """
                {
                  "content": "Blocked a blacklisted %s from joining",
                  "embeds": [
                    {
                      "title": "-------- Player Information --------",
                      "description": "**→ Name:** %s\\n**→ Origin:** %s\\n**→ Brand:** %s\\n**→ IP Address:** %s\\n**→ Protocol Version:** %s\\n**→ Host:** %s\\n**→ Rewind:** %s\\n**→ Player Type:** %s",
                      "color": 15801922,
                      "fields": [],
                      "footer": {
                        "text": "%s v%s",
                        "icon_url": "https://raw.githubusercontent.com/%s/refs/heads/main/img/icon.png"
                      }
                    }
                  ],
                  "components": [
                    {
                      "type": 1,
                      "components": [
                        {
                          "type": 2,
                          "style": 5,
                          "label": "Get the Plugin",
                          "url": "https://github.com/%s",
                          "emoji": {
                            "name": "🌐"
                          }
                        }
                      ]
                    }
                  ]
                }
              """,
          type.getAltString(),
          player.getName().replaceAll("_", "\\_"),
          player.getOrigin(),
          player.getBrand(),
          this.config.getBoolean("discord.send_ips") ? player.getAddr() : CENSORED_STR,
          player.getPVN(),
          player.getVHost(),
          // userAgent,
          player.isRewind() ? "YES" : "NO",
          connType.toString(),
          BuildInfo.get("plugin_name"),
          this.plugin.getPluginVersion() + " • " + COMMIT_S,
          PLUGIN_REPO,
          PLUGIN_REPO).getBytes();
      final Json5Array arr = this.config.get("discord.webhook_urls").getAsJson5Array();
      for (final Json5Element element : arr) {
        this.plugin.runAsync(() -> {
          try {
            final URL url = (new URI(element.getAsString())).toURL();
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            final OutputStream os = conn.getOutputStream();
            os.write(payload);
            os.close();

            final int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
              this.plugin.log(EnumLogLevel.WARN, "Webhook failed (HTTP " + code + ")");
            }

            conn.disconnect();
          } catch (final Throwable t) {
            t.printStackTrace();
          }
        });
      }
    }
  }

  private final void checkForUpdates() {
    this.checkForUpdates(() -> {
      // if (!this.config.getBoolean("update_checker.auto_update")) {
      if (true) {
        this.plugin.log(EnumLogLevel.INFO, "An update is available! Download it at " + this.updateURL);
      } else {
        this.updatePlugin();
      }
    }, () -> {
    });
  }

  private final void updateLogFile(final OriginBlacklistLoginEvent event, final EnumBlacklistType type) {
    if (this.isLogFileEnabled()) {
      final OPlayer player = event.getPlayer();
      final String txt = Instant.now() + " - [player=" + player.getName() + "," + "blacklist_reason=" + type.toString()
          + "]";
      final Path dir = Paths.get(this.getDataDir());
      try {
        Files.createDirectories(dir);
        Files.writeString(
            dir.resolve("blacklist.log"),
            txt + "\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND);
      } catch (final Throwable t) {
        t.printStackTrace();
      }
    }
  }

  public static final String getComponentString(final Component comp) {
    return LegacyComponentSerializer.legacySection().serialize(comp);
  }

  public static final String getLegacyFromMiniMessage(final String str) {
    return getComponentString(MiniMessage.miniMessage().deserialize(str));
  }

  public static final String getPNGBase64FromBytes(final byte[] bytes) {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
  }

  public static final String getUserAgent() {
    return BuildInfo.get("plugin_name") + "/" + BuildInfo.get("plugin_vers") + "+" + BuildInfo.get("git_cm_hash");
  }

  public static final boolean isNonNullStr(final String str) {
    return str != null && !str.isEmpty() && !str.isBlank() && !str.equals("null");
  }

  public static final class BSTATS {
    public static final int VELOCITY = 29033;
    public static final int BUNGEE = 29034;
    public static final int BUKKIT = 29035;
  }
}
