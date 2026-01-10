package xyz.webmc.originblacklist.bukkit.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.command.CommandContext;

import org.bukkit.command.CommandSender;

public class BKTCommandContext implements CommandContext {
  private final CommandSender sender;
  private final String[] args;

  public BKTCommandContext(final CommandSender sender, final String[] args) {
    this.sender = sender;
    this.args = args;
  }

  @Override
  public String getPlayerName() {
    return this.sender.getName();
  }

  @Override
  public void reply(final String message) {
    this.sender.sendMessage(OriginBlacklist.getLegacyFromMiniMessage(message));
  }

  @Override
  public boolean hasPermission(final String permission) {
    return this.sender.hasPermission(permission);
  }

  @Override
  public String[] getArgs() {
    return this.args;
  }
}
