package xyz.webmc.originblacklist.bungee;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.metrics.GenericMetricsAdapter;

import org.bstats.charts.CustomChart;
import org.bstats.bungeecord.Metrics;

public final class BungeeMetricsAdapter extends GenericMetricsAdapter {
  private final OriginBlacklistBungee plugin;
  private Metrics metrics;

  public BungeeMetricsAdapter(final OriginBlacklistBungee plugin) {
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
