package xyz.webmc.originblacklist.base.util;

import java.io.InputStream;
import java.util.Properties;

public class BuildInfo {
  private static final Properties properties;

  public static final String get(final String key) {
    return properties.getProperty(key).trim();
  }

  static {
    properties = new Properties();
    try (final InputStream in = BuildInfo.class.getClassLoader().getResourceAsStream("build.properties")) {
      properties.load(in);
    } catch (final Throwable t) {}
  }
}
