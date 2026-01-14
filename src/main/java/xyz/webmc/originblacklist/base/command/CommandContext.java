package xyz.webmc.originblacklist.base.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;

public interface CommandContext {
  public OriginBlacklist getPlugin();

  public String getPlayerName();

  public void reply(final String message);

  public boolean hasPermission(final String permission);

  public String[] getArgs();
}
