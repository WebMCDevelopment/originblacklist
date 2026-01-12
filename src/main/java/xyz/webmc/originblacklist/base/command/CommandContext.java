package xyz.webmc.originblacklist.base.command;

public interface CommandContext {
  public String getPlayerName();
  public void reply(final String message);
  public boolean hasPermission(final String permission);
  public String[] getArgs();
}
