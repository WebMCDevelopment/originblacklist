package xyz.webmc.originblacklist.bungee;

import xyz.webmc.originblacklist.bungee.command.OriginBlacklistCommandBungee;
import xyz.webmc.originblacklist.core.OriginBlacklist;
import xyz.webmc.originblacklist.core.enums.EnumConnectionType;
import xyz.webmc.originblacklist.core.events.OriginBlacklistLoginEvent;
import xyz.webmc.originblacklist.core.events.OriginBlacklistMOTDEvent;
import xyz.webmc.originblacklist.core.logger.JavaLogger;
import xyz.webmc.originblacklist.core.util.EaglerEventPriority;
import xyz.webmc.originblacklist.core.util.IOriginBlacklistPlugin;
import xyz.webmc.originblacklist.core.util.IncompatibleDependencyException;
import xyz.webmc.originblacklist.core.util.OPlayer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import net.kyori.adventure.text.Component;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.bungee.EaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.bungee.event.EaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.bungee.event.EaglercraftMOTDEvent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.semver4j.Semver;

@SuppressWarnings({ "deprecation", "rawtypes" })
public final class OriginBlacklistBungeePlugin extends Plugin implements Listener, IOriginBlacklistPlugin {
  private ProxyServer proxy;
  private boolean papiPlaceholdersEnabled;
  private Object papi;
  private OriginBlacklist blacklist;
  private JavaLogger logger;

  @Override
  public final void onEnable() {
    this.proxy = ProxyServer.getInstance();
    final Plugin eagx = this.getProxy().getPluginManager().getPlugin("EaglercraftXServer");
    if (eagx == null) {
      throw new IncompatibleDependencyException("EaglercraftXServer");
    } else {
      final Semver version = new Semver(eagx.getDescription().getVersion());
      if (version.isLowerThan(OriginBlacklist.REQUIRED_API_VER)) {
        throw new IncompatibleDependencyException("EaglerXServer", OriginBlacklist.REQUIRED_API_VER, version);
      }
    }
    this.papiPlaceholdersEnabled = this.getProxy().getPluginManager().getPlugin("PAPIProxyBridge") != null;
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
    this.logger = new JavaLogger(this.getLogger());
    this.blacklist = new OriginBlacklist(this);
    this.getProxy().getPluginManager().registerCommand(this,
        new OriginBlacklistCommandBungee(this, this.blacklist, "originblacklist"));
    this.getProxy().getPluginManager().registerListener(this, this);
    this.blacklist.init();
  }

  @Override
  public void onDisable() {
    this.blacklist.shutdown();
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public final void onEaglerLogin(final EaglercraftLoginEvent event) {
    final OPlayer player = new OPlayer(event.getLoginConnection(), event.getProfileUsername(), event.getProfileUUID());
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(event, null, EnumConnectionType.EAGLER, player));
  }

  @EventHandler(priority = EaglerEventPriority.EAGLER_MOTD_EVENT)
  public final void onEaglerMOTD(final EaglercraftMOTDEvent event) {
    final OPlayer player = new OPlayer(event.getMOTDConnection(), null, null);
    this.blacklist.handleMOTD(new OriginBlacklistMOTDEvent(event, null, EnumConnectionType.EAGLER, player));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public final void onJavaLogin(final PostLoginEvent event) {
    final PendingConnection conn = event.getPlayer().getPendingConnection();
    final InetSocketAddress vhost = conn.getVirtualHost();
    final ProxiedPlayer aPlayer = event.getPlayer();
    final String origin = vhost != null ? vhost.getHostString() + vhost.getPort() : OriginBlacklist.UNKNOWN_STR;
    final OPlayer bPlayer = new OPlayer(null, aPlayer.getName(), aPlayer.getUniqueId(),
        aPlayer.getAddress().toString(), aPlayer.getClientBrand(), origin,
        conn.getVersion());
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(null, event, EnumConnectionType.JAVA, bPlayer));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public final void onJavaHandshake(final PreLoginEvent event) {
    final PendingConnection conn = event.getConnection();
    final InetSocketAddress vhost = conn.getVirtualHost();
    final String origin = vhost != null ? vhost.getHostString() + vhost.getPort() : OriginBlacklist.UNKNOWN_STR;
    final OPlayer player = new OPlayer(null, null, null, conn.getAddress().toString(), OriginBlacklist.UNKNOWN_STR,
        origin, conn.getVersion());
    this.blacklist.handleLogin(new OriginBlacklistLoginEvent(null, event, EnumConnectionType.JAVA, player));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public final void onJavaMOTD(final ProxyPingEvent event) {
    final PendingConnection conn = event.getConnection();
    final InetSocketAddress vhost = conn.getVirtualHost();
    final String origin = vhost != null ? vhost.getHostString() + vhost.getPort() : OriginBlacklist.UNKNOWN_STR;
    final OPlayer player = new OPlayer(null, null, null, conn.getAddress().toString(), null,
        origin, -1);
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
  public final Path getPluginJarPath() {
    return Paths.get(this.getFile().getAbsolutePath());
  }

  @Override
  public final BungeeMetricsAdapter getMetrics() {
    return new BungeeMetricsAdapter(this);
  }

  @Override
  public final IEaglerXServerAPI getEaglerAPI() {
    return EaglerXServerAPI.instance();
  }

  @Override
  public final JavaLogger getTheLogger() {
    return this.logger;
  }

  @Override
  public final void kickPlayer(final Component comp, final OriginBlacklistLoginEvent event) {
    final String str = OriginBlacklist.getComponentString(comp);
    if (event.getConnectionType() == EnumConnectionType.EAGLER) {
      event.getEaglerEvent().setKickMessage(str);
    } else {
      final Object javaEvent = event.getJavaEvent();
      if (javaEvent instanceof PreLoginEvent preLoginEvent) {
        preLoginEvent.getConnection().disconnect(str);
      } else if (javaEvent instanceof PostLoginEvent postLoginEvent) {
        postLoginEvent.getPlayer().disconnect(str);
      }
    }
  }

  @Override
  public final void setMOTD(final Component comp, final OriginBlacklistMOTDEvent event) {
    if (event.getConnectionType() == EnumConnectionType.EAGLER) {
      this.blacklist.setEaglerMOTD(comp, event);
    } else {
      final ProxyPingEvent javaEvent = (ProxyPingEvent) event.getJavaEvent();
      final ServerPing ping = javaEvent.getResponse();
      ping.setDescription(OriginBlacklist.getComponentString(comp));
      ping.setFavicon(this.blacklist.getConfig().getIconBase64URI());
      ping.getPlayers().setOnline(0);
      ping.getPlayers().setMax(0);
      javaEvent.setResponse(ping);
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
    this.proxy.getScheduler().schedule(this, task, 0, period, unit);
  }

  @Override
  public final void runAsync(final Runnable task) {
    this.proxy.getScheduler().runAsync(this, task);
  }

  @Override
  public final void shutdown() {
    this.proxy.getScheduler().cancel(this);
  }
}
