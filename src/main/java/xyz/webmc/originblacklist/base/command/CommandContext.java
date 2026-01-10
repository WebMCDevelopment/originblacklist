package xyz.webmc.originblacklist.base.command;

public interface CommandContext {
  String getPlayerName();
  void reply(final String message);
  boolean hasPermission(final String permission);
  String[] getArgs();
}
