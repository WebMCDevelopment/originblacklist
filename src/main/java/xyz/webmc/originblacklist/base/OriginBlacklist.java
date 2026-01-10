package xyz.webmc.originblacklist.base;

import xyz.webmc.originblacklist.base.config.OriginBlacklistConfig;
import xyz.webmc.originblacklist.base.enums.EnumBlacklistType;
import xyz.webmc.originblacklist.base.enums.EnumLogLevel;
import xyz.webmc.originblacklist.base.events.OriginBlacklistLoginEvent;
import xyz.webmc.originblacklist.base.events.OriginBlacklistMOTDEvent;
import xyz.webmc.originblacklist.base.util.IOriginBlacklistPlugin;
import xyz.webmc.originblacklist.base.util.OPlayer;
import xyz.webmc.originblacklist.base.util.UpdateChecker;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.semver4j.Semver;

import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressString;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.query.IMOTDConnection;

public final class OriginBlacklist {
  public static final Semver REQUIRED_API_VER = new Semver("1.0.2");
  public static final String GENERIC_STR = "generic";
  public static final String UNKNOWN_STR = "unknown";
  public static final String PLUGIN_REPO = "WebMCDevelopment/originblacklist";
  public static final int BSTATS_ID = 28776;

  private final IOriginBlacklistPlugin plugin;
  private final OriginBlacklistConfig config;
  private boolean updateAvailable;

  public OriginBlacklist(final IOriginBlacklistPlugin plugin) {
    this.plugin = plugin;
    this.config = new OriginBlacklistConfig(plugin);
    this.checkForUpdate();
    plugin.scheduleRepeat(() -> {
      this.checkForUpdate();
    }, 60, TimeUnit.MINUTES);
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
    return this.config.get("debug").getAsBoolean();
  }
  
  public final boolean isMetricsEnabled() {
    return this.config.get("bStats").getAsBoolean();
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

  private final EnumBlacklistType testBlacklist(final OPlayer player) {
    final String name = player.getName();
    final String addr = player.getAddr();
    final String origin = player.getOrigin();
    final String brand = player.getBrand();

    if (isNonNull(origin)) {
      for (final Json5Element element : this.config.get("blacklist.origins").getAsJson5Array()) {
        if (origin.matches(element.getAsString())) {
          return EnumBlacklistType.ORIGIN;
        }
      }
    }

    if (isNonNull(brand)) {
      for (final Json5Element element : this.config.get("blacklist.brands").getAsJson5Array()) {
        if (brand.matches(element.getAsString())) {
          return EnumBlacklistType.BRAND;
        }
      }
    }

    if (isNonNull(name)) {
      for (final Json5Element element : this.config.get("blacklist.player_names").getAsJson5Array()) {
        this.plugin.log(EnumLogLevel.DEBUG, element.getAsString());
        if (name.matches(element.getAsString())) {
          return EnumBlacklistType.NAME;
        }
      }
    }

    if (isNonNull(addr)) {
      for (final Json5Element element : this.config.get("blacklist.ip_addresses").getAsJson5Array()) {
        try {
          if ((new IPAddressString(element.getAsString()).toAddress())
              .contains((new IPAddressString(addr)).toAddress())) {
            return EnumBlacklistType.ADDR;
          }
        } catch (final AddressStringException exception) {
          exception.printStackTrace();
        }
      }
    }

    return EnumBlacklistType.NONE;
  }

  private final Component getBlacklistedComponent(final String type, final String id, final String blockType,
      final String blockTypeAlt, final String notAllowed, final String notAllowedAlt, final String blockValue,
      final String action) {
    final Json5Array arr = this.config.get("messages." + type).getAsJson5Array();
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < arr.size(); i++) {
      if (i > 0)
        sb.append("\n");
      sb.append(arr.get(i).getAsString());
    }
    final String str = sb.toString()
        .replaceAll("%action%", this.config.get("messages.actions." + action).getAsString())
        .replaceAll("%block_type%", blockType)
        .replaceAll("%block_type%", blockType)
        .replaceAll("%not_allowed%", notAllowed)
        .replaceAll("%not_allowed_alt%", notAllowedAlt)
        .replaceAll("%blocked_value%", blockValue);
    return MiniMessage.miniMessage().deserialize(str);
  }

  private final void checkForUpdate() {
    (new Thread(() -> {
      this.updateAvailable = UpdateChecker.checkForUpdate(PLUGIN_REPO, this.plugin.getPluginVersion(),
          this.config.get("update_checker.allow_snapshots").getAsBoolean());
      if (this.updateAvailable) {
        this.plugin.log(EnumLogLevel.INFO, "Update Available! Download at https://github.com/" + PLUGIN_REPO + ".git");
      }
    })).run();
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

  public static final boolean isNonNull(final String str) {
    return str != null && !str.isEmpty() && !str.isBlank() && !str.equals("null");
  }
}
