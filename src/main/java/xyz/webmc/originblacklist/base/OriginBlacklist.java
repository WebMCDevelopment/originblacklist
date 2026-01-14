package xyz.webmc.originblacklist.base;

import xyz.webmc.originblacklist.base.config.OriginBlacklistConfig;
import xyz.webmc.originblacklist.base.enums.EnumBlacklistType;
import xyz.webmc.originblacklist.base.enums.EnumConnectionType;
import xyz.webmc.originblacklist.base.enums.EnumLogLevel;
import xyz.webmc.originblacklist.base.events.OriginBlacklistLoginEvent;
import xyz.webmc.originblacklist.base.events.OriginBlacklistMOTDEvent;
import xyz.webmc.originblacklist.base.util.BuildInfo;
import xyz.webmc.originblacklist.base.util.IOriginBlacklistPlugin;
import xyz.webmc.originblacklist.base.util.OPlayer;
import xyz.webmc.originblacklist.base.util.UpdateChecker;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.EnumWebSocketHeader;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerLoginConnection;
import net.lax1dude.eaglercraft.backend.server.api.query.IMOTDConnection;
import org.semver4j.Semver;

public final class OriginBlacklist {
  public static final Semver REQUIRED_API_VER = new Semver("1.0.2");
  public static final String GENERIC_STR = "GENERIC";
  public static final String UNKNOWN_STR = "UNKNOWN";
  public static final String PLUGIN_REPO = "WebMCDevelopment/originblacklist";
  public static final int BSTATS_ID = 28776;

  private final IOriginBlacklistPlugin plugin;
  private final OriginBlacklistConfig config;
  private String updateURL;

  public OriginBlacklist(final IOriginBlacklistPlugin plugin) {
    this.plugin = plugin;
    this.config = new OriginBlacklistConfig(plugin);
    plugin.scheduleRepeat(() -> {
      this.checkForUpdates();
    }, this.config.getInteger("update_checker.check_timer"), TimeUnit.SECONDS);
  }

  public final void init() {
    this.plugin.log(EnumLogLevel.INFO, "Initialized Plugin");
    this.plugin.log(EnumLogLevel.DEBUG, "Commit " + BuildInfo.get("git_cm_hash"));
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
          blacklisted_value, blacklisted.getActionString()), event);
      this.sendWebhooks(event, blacklisted);
      final String name = player.getName();
      if (isNonNull(name)) {
        this.plugin.log(EnumLogLevel.INFO, "Prevented blacklisted player " + name + " from joining.");
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
      this.plugin.setMOTD(this.getBlacklistedComponent("motd", blacklisted.getArrayString(), blacklisted.getAltString(),
          blacklisted.getString(), "blacklisted", "blacklisted from the server", blacklisted_value,
          blacklisted.getActionString()), event);
    }
  }

  public final boolean isDebugEnabled() {
    return this.config.getBoolean("debug");
  }

  public final boolean isMetricsEnabled() {
    return this.config.getBoolean("bStats");
  }

  public final OriginBlacklistConfig getConfig() {
    return this.config;
  }

  public final void setEaglerMOTD(final Component comp, final OriginBlacklistMOTDEvent event) {
    final IMOTDConnection conn = event.getEaglerEvent().getMOTDConnection();
    final List<String> lst = new ArrayList<>();
    for (String ln : getComponentString(comp).split("\n")) {
      lst.add(ln);
    }
    conn.setServerMOTD(lst);
    conn.setPlayerTotal(0);
    conn.setPlayerUnlimited();
    conn.setPlayerList(List.of());
    conn.setServerIcon(this.config.getIconBytes());
    conn.sendToUser();
    conn.disconnect();
  }

  public final void checkForUpdates(Runnable action1, Runnable action2) {
    if (this.config.getBoolean("update_checker.enabled")) {
      this.plugin.runAsync(() -> {
        this.updateURL = UpdateChecker.checkForUpdates(PLUGIN_REPO, this.plugin.getPluginVersion(),
            this.config.getBoolean("update_checker.allow_snapshots"));
        if (isNonNull((this.updateURL))) {
          action1.run();
          return;
        }
        action2.run();
      });
    } else {
      action2.run();
    }
  }

  public final void updatePlugin(Runnable action1, Runnable action2) {
    try {
      final URL url = new URL(this.updateURL);
      final Path jar = this.plugin.getPluginJarPath();
      final Path bak = jar.resolveSibling(jar.getFileName().toString() + ".bak");
      final Path upd = jar.resolveSibling(Paths.get(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8)).getFileName());

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
        Files.delete(jar);
        Files.delete(bak);
        action1.run();
        return;
      } catch (final Throwable t) {
        t.printStackTrace();
        Files.move(bak, jar, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
    action2.run();
  }

  public final void updatePlugin() {
    this.updatePlugin(() -> {}, () -> {});
  }

  private final EnumBlacklistType testBlacklist(final OPlayer player) {
    final String name = player.getName();
    final String addr = player.getAddr();
    final String origin = player.getOrigin();
    final String brand = player.getBrand();

    final boolean whitelist = this.config.getBoolean("blacklist_to_whitelist");
    EnumBlacklistType type = EnumBlacklistType.NONE;

    if (isNonNull(origin)) {
      if (whitelist && !type.isBlacklisted()) type = EnumBlacklistType.ORIGIN;
      for (final Json5Element element : this.config.getArray("blacklist.origins").getAsJson5Array()) {
        if (origin.matches(element.getAsString())) {
          if (whitelist) type = EnumBlacklistType.NONE;
          else if (!type.isBlacklisted()) type = EnumBlacklistType.ORIGIN;
          break;
        }
      }
    } else if (this.config.getBoolean("block_undefined_origin")) {
      return whitelist ? EnumBlacklistType.NONE : EnumBlacklistType.ORIGIN;
    }

    if (isNonNull(brand)) {
      if (whitelist && !type.isBlacklisted()) type = EnumBlacklistType.BRAND;
      for (final Json5Element element : this.config.getArray("blacklist.brands")) {
        if (brand.matches(element.getAsString())) {
          if (whitelist) type = EnumBlacklistType.NONE;
          else if (!type.isBlacklisted()) type = EnumBlacklistType.BRAND;
          break;
        }
      }
    }

    if (isNonNull(name)) {
      if (whitelist && !type.isBlacklisted()) type = EnumBlacklistType.NAME;
      for (final Json5Element element : this.config.getArray("blacklist.player_names")) {
        this.plugin.log(EnumLogLevel.DEBUG, element.getAsString());
        if (name.matches(element.getAsString())) {
          if (whitelist) type = EnumBlacklistType.NONE;
          else if (!type.isBlacklisted()) type = EnumBlacklistType.NAME;
          break;
        }
      }
    }

    if (isNonNull(addr)) {
      if (whitelist && !type.isBlacklisted()) type = EnumBlacklistType.ADDR;
      for (final Json5Element element : this.config.getArray("blacklist.ip_addresses")) {
        try {
          if ((new IPAddressString(element.getAsString()).toAddress())
              .contains((new IPAddressString(addr)).toAddress())) {
            if (whitelist) type = EnumBlacklistType.NONE;
            else if (!type.isBlacklisted()) type = EnumBlacklistType.ADDR;
            break;
          }
        } catch (final AddressStringException exception) {
          if (this.isDebugEnabled()) exception.printStackTrace();
        }
      }
    }

    return type;
  }

  private final Component getBlacklistedComponent(final String type, final String id, final String blockType,
      final String blockTypeAlt, final String notAllowed, final String notAllowedAlt, final String blockValue,
      final String action) {
    final Json5Array arr = this.config.getArray("messages." + type);
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
        .replaceAll("%blocked_value%", blockValue);
    return MiniMessage.miniMessage().deserialize(str);
  }

  private final void sendWebhooks(final OriginBlacklistLoginEvent event, final EnumBlacklistType type) {
    if (this.config.getBoolean("discord.enabled")) {
      final OPlayer player = event.getPlayer();
      final EnumConnectionType connType = event.getConnectionType();
      final String userAgent;
      if (connType == EnumConnectionType.EAGLER) {
        final IEaglerLoginConnection loginConn = event.getEaglerEvent().getLoginConnection();
        userAgent = loginConn.getWebSocketHeader(EnumWebSocketHeader.HEADER_USER_AGENT);
      } else {
        userAgent = UNKNOWN_STR;
      }
      final byte[] payload = String.format(
        """
          {
            "content": "Blocked a blacklisted %s from joining",
            "embeds": [
              {
                "title": "-------- Player Information --------",
                "description": "**→ Name:** %s\\n**→ Origin:** %s\\n**→ Brand:** %s\\n**→ IP Address:** %s\\n**→ Protocol Version:** %s\\n**→ User Agent:** %s\\n**→ Rewind:** %s\\n**→ Player Type:** %s",
                "color": 15801922,
                "fields": [],
                "footer": {
                  "text": "OriginBlacklist v%s",
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
        this.config.getBoolean("discord.send_ips") ? player.getAddr() : "*\\*CENSORED\\**",
        player.getPVN(),
        userAgent,
        player.isRewind() ? "YES" : "NO",
        connType.toString(),
        this.plugin.getPluginVersion(),
        PLUGIN_REPO,
        PLUGIN_REPO
      ).getBytes();
      final Json5Element element = this.config.get("discord.webhook_urls");
      if (element instanceof Json5Array) {
        for (final Json5Element _element : element.getAsJson5Array()) {
          this.plugin.runAsync(() -> {
            try {
              final URL url = new URL(_element.getAsString());
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
  }

  private final void checkForUpdates() {
    this.checkForUpdates(() -> {
      if (!this.config.getBoolean("update_checker.auto_update")) {
        this.plugin.log(EnumLogLevel.INFO, "An update is available! Download it at " + this.updateURL);
      } else {
        this.updatePlugin();
      }
    }, () -> {});
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

  public static final boolean isNonNull(final String str) {
    return str != null && !str.isEmpty() && !str.isBlank() && !str.equals("null");
  }
}
