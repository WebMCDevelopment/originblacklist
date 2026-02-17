package xyz.webmc.originblacklist.velocity;

import xyz.webmc.originblacklist.core.OriginBlacklist;
import xyz.webmc.originblacklist.core.metrics.GenericMetricsAdapter;

import org.bstats.charts.CustomChart;
import org.bstats.velocity.Metrics;
import org.bstats.velocity.Metrics.Factory;

public final class VelocityMetricsAdapter extends GenericMetricsAdapter {
  private final OriginBlacklistVelocityPlugin plugin;
  private final Factory factory;
  private Metrics metrics;

  public VelocityMetricsAdapter(final OriginBlacklistVelocityPlugin plugin, final Factory factory) {
    super();
    this.plugin = plugin;
    this.factory = factory;
  }

  @Override
  public void start() {
    if (this.metrics == null) {
      this.metrics = this.factory.make(this.plugin, OriginBlacklist.BSTATS.VELOCITY);
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
