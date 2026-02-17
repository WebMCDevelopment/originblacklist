package xyz.webmc.originblacklist.bungee;

import org.bstats.charts.CustomChart;

import xyz.webmc.originblacklist.core.OriginBlacklist;
import xyz.webmc.originblacklist.core.metrics.GenericMetricsAdapter;

import org.bstats.bungeecord.Metrics;

public final class BungeeMetricsAdapter extends GenericMetricsAdapter {
  private final OriginBlacklistBungeePlugin plugin;
  private Metrics metrics;

  public BungeeMetricsAdapter(final OriginBlacklistBungeePlugin plugin) {
    super();
    this.plugin = plugin;
  }

  @Override
  public void start() {
    if (this.metrics == null) {
      this.metrics = new Metrics(this.plugin, OriginBlacklist.BSTATS.BUNGEE);
      for (final CustomChart chart : this.charts) {
        this.metrics.addCustomChart(chart);
      }
    }
  }

  @Override
  public void shutdown() {
    if (this.metrics != null) {
      this.metrics.shutdown();
      this.metrics = null;
    }
  }
}
