package xyz.webmc.originblacklist.base.command;

import java.util.List;

public interface ICommand {
  public static final String NO_PERMISSION = "<red>You don't have permission to use this command.</red>";

  public boolean execute(final CommandContext ctx);

  public List<String> suggest(final CommandContext ctx);

  public void usage(final CommandContext ctx);
}
