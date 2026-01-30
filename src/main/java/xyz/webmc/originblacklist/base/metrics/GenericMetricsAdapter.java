package xyz.webmc.originblacklist.base.metrics;

import java.util.ArrayList;
import java.util.List;

import org.bstats.charts.CustomChart;

public abstract class GenericMetricsAdapter {
  protected final List<CustomChart> charts;

  protected GenericMetricsAdapter() {
    this.charts = new ArrayList<>();
  }
  
  public final void addCustomChart(final CustomChart chart) {
    this.charts.add(chart);
  }

  public abstract void start();
  public abstract void shutdown();
}
