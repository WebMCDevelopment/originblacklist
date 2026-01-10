package xyz.webmc.originblacklist.base.command;

import java.util.List;

public interface ICommand {
  static final String NO_PERMISSION = "<red>You don't have permission to use this command.</red>";
  boolean execute(final CommandContext ctx);
  List<String> suggest(final CommandContext ctx);
  void usage(final CommandContext ctx);
}
