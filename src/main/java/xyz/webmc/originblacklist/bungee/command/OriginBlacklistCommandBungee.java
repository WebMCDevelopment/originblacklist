package xyz.webmc.originblacklist.bungee.command;

import xyz.webmc.originblacklist.bungee.OriginBlacklistBungeePlugin;
import xyz.webmc.originblacklist.core.OriginBlacklist;
import xyz.webmc.originblacklist.core.command.OriginBlacklistCommand;

import java.util.List;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public final class OriginBlacklistCommandBungee extends Command implements TabExecutor {
  private final OriginBlacklistCommand cmd;
  private final OriginBlacklist blacklist;

  public OriginBlacklistCommandBungee(final OriginBlacklistBungeePlugin plugin, final OriginBlacklist blacklist,
      final String command) {
    super(command);
    this.cmd = new OriginBlacklistCommand(blacklist);
    this.blacklist = blacklist;
  }

  @Override
  public final void execute(final CommandSender sender, final String[] args) {
    this.cmd.execute(new BNGCommandContext(this.blacklist, sender, args));
  }

  @Override
  public final List<String> onTabComplete(final CommandSender sender, final String[] args) {
    return this.cmd.suggest(new BNGCommandContext(this.blacklist, sender, args));
  }
}
