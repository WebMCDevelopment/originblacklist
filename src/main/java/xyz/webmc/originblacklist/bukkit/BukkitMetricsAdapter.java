package xyz.webmc.originblacklist.bukkit;

import org.bstats.charts.CustomChart;

import xyz.webmc.originblacklist.core.OriginBlacklist;
import xyz.webmc.originblacklist.core.metrics.GenericMetricsAdapter;

import org.bstats.bukkit.Metrics;

public final class BukkitMetricsAdapter extends GenericMetricsAdapter {
  private final OriginBlacklistBukkitPlugin plugin;
  private Metrics metrics;

  public BukkitMetricsAdapter(final OriginBlacklistBukkitPlugin plugin) {
    super();
    this.plugin = plugin;
  }

  @Override
  public void start() {
    if (this.metrics == null) {
      this.metrics = new Metrics(this.plugin, OriginBlacklist.BSTATS.BUKKIT);
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
