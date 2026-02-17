package xyz.webmc.originblacklist.core.logger;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class JavaLogger extends AbstractLogger {
  private final Logger logger;

  public JavaLogger(final Logger logger) {
    this.logger = logger;
  }

  @Override
  public final void info(final String txt, final Object ...args) {
    this.logger.log(Level.INFO, txt, args);
  }

  @Override
  public final void warn(final String txt, final Object ...args) {
    this.logger.log(Level.WARNING, txt, args);
  }

  @Override
  public final void error(final String txt, final Object ...args) {
    this.logger.log(Level.SEVERE, txt, args);
  }
}
