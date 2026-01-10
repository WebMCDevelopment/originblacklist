package xyz.webmc.originblacklist.base.util;

import org.semver4j.Semver;

public final class IncompatibleDependencyException extends RuntimeException {
  public IncompatibleDependencyException(final String name, final Semver requiredVersion, final Semver currentVersion) {
    super("Incompatible version of " + name + " is present! Required " + requiredVersion + ", but found "
        + currentVersion + ".");
  }

  public IncompatibleDependencyException(final String name) {
    super("Missing dependency " + name + "!");
  }
}
