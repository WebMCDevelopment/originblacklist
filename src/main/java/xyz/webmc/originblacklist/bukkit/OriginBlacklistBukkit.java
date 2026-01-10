package xyz.webmc.originblacklist.bukkit;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.enums.EnumConnectionType;
import xyz.webmc.originblacklist.base.enums.EnumLogLevel;
import xyz.webmc.originblacklist.base.events.OriginBlacklistLoginEvent;
import xyz.webmc.originblacklist.base.events.OriginBlacklistMOTDEvent;
import xyz.webmc.originblacklist.base.util.OPlayer;
import xyz.webmc.originblacklist.base.util.IOriginBlacklistPlugin;
import xyz.webmc.originblacklist.base.util.IncompatibleDependencyException;
import xyz.webmc.originblacklist.bukkit.command.OriginBlacklistCommandBukkit;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.semver4j.Semver;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;

import net.kyori.adventure.text.Component;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.bukkit.EaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.bukkit.event.EaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.bukkit.event.EaglercraftMOTDEvent;

public final class OriginBlacklistBukkit extends JavaPlugin implements Listener, IOriginBlacklistPlugin {
  private boolean papiPlaceholdersEnabled;
  private Object papi;
  private OriginBlacklist blacklist;
  private IEaglerXServerAPI eaglerAPI;
  private Metrics metrics;

  private CachedServerIcon iconCache;

  @Override
  public final void onEnable() {
    final Plugin eagx = this.getServer().getPluginManager().getPlugin("EaglercraftXServer");
    if (eagx == null) {
      throw new IncompatibleDependencyException("EaglercraftXServer");
    } else {
      final Semver version = new Semver(eagx.getDescription().getVersion());
      if (version.isLowerThan(OriginBlacklist.REQUIRED_API_VER)) {
        throw new IncompatibleDependencyException("EaglerXServer", OriginBlacklist.REQUIRED_API_VER, version);
      }
    }
    this.papiPlaceholdersEnabled = this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    if (this.papiPlaceholdersEnabled) {
      try {
        this.papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      } catch (final Throwable t) {
        this.papi = null;
        this.papiPlaceholdersEnabled = false;
      }
    } else {
      this.papi = null;
    }
    this.blacklist = new OriginBlacklist(this);
    this.eaglerAPI = EaglerXServerAPI.instance();
    this.getCommand("originblacklist").setExecutor(new OriginBlacklistCommandBukkit(this.blacklist));
    this.getServer().getPluginManager().registerEvents(this, this);
    this.log(EnumLogLevel.INFO, "Initialized Plugin");
    if (this.blacklist.isMetricsEnabled()) {
      this.metrics = new Metrics(this, OriginBlacklist.BSTATS_ID);
      this.metrics.addCustomChart(new AdvancedPie("player_types", () -> {
        final Map<String, Integer> playerMap = new HashMap<>();

        for (final Player player : Bukkit.getOnlinePlayers()) {
          final boolean eagler = eaglerAPI.isEaglerPlayerByUUID(player.getUniqueId());
          final String key = eagler ? "Eagler" : "Java";
          playerMap.put(key, playerMap.getOrDefault(key, 0) + 1);
        }

        return playerMap;
      }));
    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public final void onEaglerLogin(final EaglercraftLoginEvent event) {
    final OPlayer player = new OPlayer(event.getLoginConnection(), event.getProfileUsername(), event.getProfileUUID());
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(event, null, EnumConnectionType.EAGLER, player));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public final void onEaglerMOTD(final EaglercraftMOTDEvent event) {
    final OPlayer player = new OPlayer(event.getMOTDConnection(), null, null);
    this.blacklist.handleMOTD(new OriginBlacklistMOTDEvent(event, null, EnumConnectionType.EAGLER, player));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public final void onJavaLogin(final AsyncPlayerPreLoginEvent event) {
    final OPlayer player = new OPlayer(null, event.getName(), event.getUniqueId(),
        event.getAddress() != null ? event.getAddress().toString() : null, OriginBlacklist.UNKNOWN_STR, -1);
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(null, event, EnumConnectionType.JAVA, player));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public final void onJavaMOTD(final ServerListPingEvent event) {
    final OPlayer player = new OPlayer(null, null, null,
        event.getAddress() != null ? event.getAddress().toString() : null, null, -1);
    this.blacklist.handleMOTD(new OriginBlacklistMOTDEvent(null, event, EnumConnectionType.JAVA, player));
  }

  @Override
  public final String getPluginId() {
    return this.getDescription().getName();
  }

  @Override
  public final Semver getPluginVersion() {
    return new Semver(this.getDescription().getVersion());
  }

  @Override
  public final void log(final EnumLogLevel level, final String txt) {
    if (level == EnumLogLevel.WARN) {
      this.getLogger().warning(txt);
    } else if (level == EnumLogLevel.ERROR) {
      this.getLogger().severe(txt);
    } else if (level == EnumLogLevel.DEBUG) {
      if (this.blacklist != null && this.blacklist.isDebugEnabled()) {
        this.getLogger().info(txt);
      }
    } else {
      this.getLogger().info(txt);
    }
  }

  @Override
  public final void kickPlayer(final Component comp, final OriginBlacklistLoginEvent event) {
    if (event.getConnectionType() == EnumConnectionType.EAGLER) {
      event.getEaglerEvent().setKickMessage(OriginBlacklist.getComponentString(comp));
    } else {
      final Object javaEvent = event.getJavaEvent();
      final String msg = OriginBlacklist.getComponentString(comp);
      if (javaEvent instanceof AsyncPlayerPreLoginEvent pre) {
        pre.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, msg);
      } else if (javaEvent instanceof PlayerJoinEvent join) {
        join.getPlayer().kickPlayer(msg);
      }
    }
  }

  @Override
  public final void setMOTD(final Component comp, final OriginBlacklistMOTDEvent event) {
    if (event.getConnectionType() == EnumConnectionType.EAGLER) {
      this.blacklist.setEaglerMOTD(comp, event);
    } else {
      final ServerListPingEvent javaEvent = (ServerListPingEvent) event.getJavaEvent();
      javaEvent.setMotd(OriginBlacklist.getComponentString(comp));
      javaEvent.setMaxPlayers(0);
      final CachedServerIcon icon = this.loadIcon();
      if (icon != null) {
        try {
          javaEvent.setServerIcon(icon);
        } catch (final Throwable t) {
        }
      }
    }
  }

  private final CachedServerIcon loadIcon() {
    if (this.iconCache != null)
      return this.iconCache;
    final String uri = this.blacklist.getConfig().getIconBase64URI();
    if (uri == null || uri.isEmpty())
      return null;
    try {
      String b64 = uri;
      final int i = b64.indexOf("base64,");
      if (i != -1)
        b64 = b64.substring(i + "base64,".length());
      final byte[] png = Base64.getDecoder().decode(b64);
      final BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(png));
      if (img != null) {
        try {
          this.iconCache = Bukkit.loadServerIcon(img);
        } catch (final Throwable t) {
          return null;
        }
      } else {
        return null;
      }

      return this.iconCache;
    } catch (final Throwable t) {
      return null;
    }
  }

  @Override
  public final String parsePlaceholders(final OPlayer player, final String txt) {
    if (this.papiPlaceholdersEnabled) {
      try {
        final UUID uuid = player.getUUID();
        final Player bp = uuid != null ? (Player) Bukkit.getPlayer(uuid) : null;
        if (bp != null) {
          return (String) ((Class<?>) this.papi)
              .getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class).invoke(null, bp, txt);
        }
      } catch (final Throwable t) {
      }
    }
    return txt;
  }

  @Override
  public final void scheduleRepeat(final Runnable task, final int period, final TimeUnit unit) {
    long ms = unit.toMillis((long) period);
    long ticks = Math.max(1L, ms / 50L);
    Bukkit.getScheduler().runTaskTimer(this, task, ticks, ticks);
  }

  @Override
  public final void shutdown() {
    this.metrics.shutdown();
    Bukkit.getScheduler().cancelTasks(this);
  }
}
