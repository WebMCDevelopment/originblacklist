package xyz.webmc.originblacklist.base.util;

import xyz.webmc.originblacklist.base.enums.EnumLogLevel;
import xyz.webmc.originblacklist.base.events.OriginBlacklistLoginEvent;
import xyz.webmc.originblacklist.base.events.OriginBlacklistMOTDEvent;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import net.kyori.adventure.text.Component;
import org.semver4j.Semver;

public interface IOriginBlacklistPlugin {
  public String getPluginId();

  public Semver getPluginVersion();

  public Path getPluginJarPath();

  public void log(final EnumLogLevel level, final String txt);

  public void kickPlayer(final Component txt, final OriginBlacklistLoginEvent event);

  public void setMOTD(final Component txt, final OriginBlacklistMOTDEvent event);

  public String parsePlaceholders(final OPlayer player, final String str);

  public void scheduleRepeat(final Runnable task, final int period, final TimeUnit unit);

  public void runAsync(final Runnable task);

  public void shutdown();
}
