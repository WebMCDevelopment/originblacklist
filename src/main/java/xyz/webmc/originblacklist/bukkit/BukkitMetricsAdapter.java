package xyz.webmc.originblacklist.bukkit;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.metrics.GenericMetricsAdapter;

import org.bstats.charts.CustomChart;
import org.bstats.bukkit.Metrics;

public final class BukkitMetricsAdapter extends GenericMetricsAdapter {
  private final OriginBlacklistBukkit plugin;
  private Metrics metrics;

  public BukkitMetricsAdapter(final OriginBlacklistBukkit plugin) {
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
