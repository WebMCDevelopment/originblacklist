package xyz.webmc.originblacklist.bungee.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.command.CommandContext;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

public class BNGCommandContext implements CommandContext {
  private final OriginBlacklist plugin;
  private final CommandSender sender;
  private final String[] args;

  public BNGCommandContext(final OriginBlacklist plugin, final CommandSender sender, final String[] args) {
    this.plugin = plugin;
    this.sender = sender;
    this.args = args;
  }

  @Override
  public OriginBlacklist getPlugin() {
    return this.plugin;
  }

  @Override
  public String getPlayerName() {
    return this.sender.getName();
  }

  @Override
  public void reply(final String message) {
    this.sender.sendMessage(TextComponent.fromLegacy(OriginBlacklist.getLegacyFromMiniMessage(message)));
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
