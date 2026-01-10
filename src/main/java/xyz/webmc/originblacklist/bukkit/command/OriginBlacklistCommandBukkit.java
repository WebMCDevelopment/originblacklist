package xyz.webmc.originblacklist.bukkit.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.command.OriginBlacklistCommand;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class OriginBlacklistCommandBukkit extends OriginBlacklistCommand implements TabExecutor {
  public OriginBlacklistCommandBukkit(OriginBlacklist plugin) {
    super(plugin);
  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
    return super.execute(new BKTCommandContext(sender, args));
  }

  @Override
  public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
    return super.suggest(new BKTCommandContext(sender, args));
  }
}
