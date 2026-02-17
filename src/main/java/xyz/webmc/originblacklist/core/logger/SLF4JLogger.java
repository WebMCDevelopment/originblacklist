package xyz.webmc.originblacklist.core.logger;

import org.slf4j.Logger;

public final class SLF4JLogger extends AbstractLogger {
  private final Logger logger;

  public SLF4JLogger(final Logger logger) {
    this.logger = logger;
  }

  @Override
  public final void info(final String txt, final Object ...args) {
    this.logger.info(txt, args);
  }

  @Override
  public final void warn(final String txt, final Object ...args) {
    this.logger.warn(txt, args);
  }

  @Override
  public final void error(final String txt, final Object ...args) {
    this.logger.error(txt, args);
  }
}
