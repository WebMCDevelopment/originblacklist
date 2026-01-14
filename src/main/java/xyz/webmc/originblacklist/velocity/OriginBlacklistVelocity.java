package xyz.webmc.originblacklist.velocity;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.enums.EnumConnectionType;
import xyz.webmc.originblacklist.base.enums.EnumLogLevel;
import xyz.webmc.originblacklist.base.events.OriginBlacklistLoginEvent;
import xyz.webmc.originblacklist.base.events.OriginBlacklistMOTDEvent;
import xyz.webmc.originblacklist.base.util.IOriginBlacklistPlugin;
import xyz.webmc.originblacklist.base.util.IncompatibleDependencyException;
import xyz.webmc.originblacklist.base.util.OPlayer;
import xyz.webmc.originblacklist.velocity.command.OriginBlacklistCommandVelocity;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.velocity.EaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.velocity.event.EaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.velocity.event.EaglercraftMOTDEvent;
import org.bstats.charts.AdvancedPie;
import org.bstats.velocity.Metrics;
import org.bstats.velocity.Metrics.Factory;
import org.semver4j.Semver;
import org.slf4j.Logger;

@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
public final class OriginBlacklistVelocity implements IOriginBlacklistPlugin {
  private final PluginContainer plugin;
  private final Factory metricsFactory;
  private final ProxyServer proxy;
  private final Logger logger;

  private boolean papiPlaceholdersEnabled;
  private Object papi;
  private OriginBlacklist blacklist;
  private IEaglerXServerAPI eaglerAPI;
  private Metrics metrics;

  @Inject
  public OriginBlacklistVelocity(final PluginContainer plugin, Factory metricsFactory, final ProxyServer proxy,
      final Logger logger) {
    this.plugin = plugin;
    this.metricsFactory = metricsFactory;
    this.proxy = proxy;
    this.logger = logger;
  }

  @Subscribe
  public final void onProxyInitialization(ProxyInitializeEvent event) {
    this.proxy.getPluginManager().getPlugin("eaglerxserver").ifPresentOrElse(plugin -> {
      final Semver version = new Semver(plugin.getDescription().getVersion().orElse("1.0.0"));
      if (version.isLowerThan(OriginBlacklist.REQUIRED_API_VER)) {
        throw new IncompatibleDependencyException("EaglerXServer", OriginBlacklist.REQUIRED_API_VER, version);
      }
    }, () -> {
      throw new IncompatibleDependencyException("EaglerXServer");
    });
    this.papiPlaceholdersEnabled = this.proxy.getPluginManager().getPlugin("papiproxybridge").isPresent();
    if (this.papiPlaceholdersEnabled) {
      try {
        this.papi = Class.forName("net.william278.papiproxybridge.api.PlaceholderAPI").getMethod("createInstance")
            .invoke(null);
      } catch (final Throwable t) {
        this.papi = null;
        this.papiPlaceholdersEnabled = false;
      }
    } else {
      this.papi = null;
    }
    this.blacklist = new OriginBlacklist(this);
    this.eaglerAPI = EaglerXServerAPI.instance();
    this.proxy.getCommandManager().register("originblacklist", new OriginBlacklistCommandVelocity(this.blacklist));
    this.blacklist.init();
    if (this.blacklist.isMetricsEnabled()) {
      this.metrics = this.metricsFactory.make(this, OriginBlacklist.BSTATS_ID);
      this.metrics.addCustomChart(new AdvancedPie("player_types", () -> {
        final Map<String, Integer> playerMap = new HashMap<>();

        for (final Player player : this.proxy.getAllPlayers()) {
          final boolean eagler = eaglerAPI.isEaglerPlayerByUUID(player.getUniqueId());
          final String key = eagler ? "Eagler" : "Java";
          playerMap.put(key, playerMap.getOrDefault(key, 0) + 1);
        }

        return playerMap;
      }));
    }
  }

  @Subscribe(order = PostOrder.FIRST)
  public final void onEaglerLogin(final EaglercraftLoginEvent event) {
    final OPlayer player = new OPlayer(event.getLoginConnection(), event.getProfileUsername(), event.getProfileUUID());
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(event, null, EnumConnectionType.EAGLER, player));
  }

  @Subscribe(order = PostOrder.LAST)
  public final void onEaglerMOTD(final EaglercraftMOTDEvent event) {
    final OPlayer player = new OPlayer(event.getMOTDConnection(), null, null);
    this.blacklist.handleMOTD(new OriginBlacklistMOTDEvent(event, null, EnumConnectionType.EAGLER, player));
  }

  @Subscribe(order = PostOrder.FIRST)
  public final void onJavaLogin(final PreLoginEvent event) {
    final OPlayer player = new OPlayer(null, event.getUsername(), event.getUniqueId(),
        event.getConnection().getRemoteAddress().toString(), OriginBlacklist.UNKNOWN_STR,
        event.getConnection().getProtocolVersion().getProtocol());
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(null, event, EnumConnectionType.JAVA, player));
  }

  @Subscribe(order = PostOrder.FIRST)
  public final void onJavaHandshake(final PlayerClientBrandEvent event) {
    final Player aPlayer = (Player) event.getPlayer();
    final OPlayer bPlayer = new OPlayer(null, aPlayer.getUsername(), aPlayer.getUniqueId(),
        aPlayer.getRemoteAddress().getAddress().toString(), event.getBrand(),
        event.getPlayer().getProtocolVersion().getProtocol());
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(null, event, EnumConnectionType.JAVA, bPlayer));
  }

  @Subscribe(order = PostOrder.LAST)
  public final void onJavaMOTD(final ProxyPingEvent event) {
    final OPlayer player = new OPlayer(null, null, null, event.getConnection().getRemoteAddress().getHostString(),
        null, -1);
    this.blacklist.handleMOTD(new OriginBlacklistMOTDEvent(null, event, EnumConnectionType.JAVA, player));
  }

  @Override
  public final String getPluginId() {
    return this.plugin.getDescription().getId();
  }

  @Override
  public final Semver getPluginVersion() {
    return new Semver(this.plugin.getDescription().getVersion().get());
  }

  @Override
  public final Path getPluginJarPath() {
    try {
      return Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
    } catch (final Throwable t) {
      throw new RuntimeException("Unable to determine plugin JAR path");
    }
  }

  @Override
  public final void log(final EnumLogLevel level, final String txt) {
    if (level == EnumLogLevel.WARN) {
      this.logger.warn(txt);
    } else if (level == EnumLogLevel.ERROR) {
      this.logger.error(txt);
    } else if (level == EnumLogLevel.DEBUG) {
      if (this.blacklist.isDebugEnabled()) {
        this.logger.info(txt);
      }
    } else {
      this.logger.info(txt);
    }
  }

  @Override
  public final void kickPlayer(final Component comp, final OriginBlacklistLoginEvent event) {
    if (event.getConnectionType() == EnumConnectionType.EAGLER) {
      event.getEaglerEvent().setKickMessage(comp);
    } else {
      final Object javaEvent = event.getJavaEvent();
      if (javaEvent instanceof PreLoginEvent loginEvent) {
        loginEvent.setResult(PreLoginEvent.PreLoginComponentResult.denied(comp));
      } else if (javaEvent instanceof PlayerClientBrandEvent brandEvent) {
        brandEvent.getPlayer().disconnect(comp);
      }
    }
  }

  @Override
  public final void setMOTD(final Component comp, final OriginBlacklistMOTDEvent event) {
    if (event.getConnectionType() == EnumConnectionType.EAGLER) {
      blacklist.setEaglerMOTD(comp, event);
    } else {
      final ProxyPingEvent javaEvent = (ProxyPingEvent) event.getJavaEvent();
      ServerPing ping = ServerPing.builder()
          .description(comp)
          .version(new ServerPing.Version(0, ""))
          .samplePlayers(List.of())
          .onlinePlayers(0)
          .maximumPlayers(0)
          .favicon(new Favicon(this.blacklist.getConfig().getIconBase64URI()))
          .build();
      javaEvent.setPing(ping);
    }
  }

  @Override
  public final String parsePlaceholders(final OPlayer player, final String txt) {
    if (this.papiPlaceholdersEnabled && this.papi != null) {
      try {
        return (String) this.papi.getClass().getMethod("formatPlaceholders", String.class, java.util.UUID.class)
            .invoke(this.papi, txt, player.getUUID());
      } catch (final Throwable t) {
      }
    }
    return txt;
  }

  @Override
  public final void scheduleRepeat(final Runnable task, final int period, final TimeUnit unit) {
    this.proxy.getScheduler()
        .buildTask(this, task)
        .repeat(period, unit)
        .schedule();
  }

  @Override
  public final void runAsync(final Runnable task) {
    this.proxy.getScheduler()
        .buildTask(this, task)
        .schedule();
  }

  @Override
  public final void shutdown() {
    this.metrics.shutdown();
    for (ScheduledTask task : this.proxy.getScheduler().tasksByPlugin(this.plugin)) {
      task.cancel();
    }
  }
}
