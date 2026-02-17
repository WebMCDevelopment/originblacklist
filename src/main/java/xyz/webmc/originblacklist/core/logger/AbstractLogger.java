package xyz.webmc.originblacklist.core.logger;

import xyz.webmc.originblacklist.core.OriginBlacklist;

public abstract class AbstractLogger {
  private OriginBlacklist plugin = null;
  public abstract void info(final String txt, final Object ...args);
  public abstract void warn(final String txt, final Object ...args);
  public abstract void error(final String txt, final Object ...args);
  public final AbstractLogger setPlugin(final OriginBlacklist plugin) {
    this.plugin = plugin;
    return this;
  }
  public final void debug(final String txt, final Object ...args) {
    if (this.plugin.isDebugEnabled()) {
      this.info(txt, args);
    }
  }
}
