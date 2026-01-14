package xyz.webmc.originblacklist.velocity.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.command.CommandContext;

import com.velocitypowered.api.command.SimpleCommand.Invocation;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class VCommandContext implements CommandContext {
  private final OriginBlacklist plugin;
  private final Invocation invocation;

  public VCommandContext(final OriginBlacklist plugin, final Invocation invocation) {
    this.plugin = plugin;
    this.invocation = invocation;
  }

  @Override
  public final OriginBlacklist getPlugin() {
    return this.plugin;
  }

  @Override
  public final String getPlayerName() {
    return this.invocation.source().toString();
  }

  @Override
  public final void reply(final String message) {
    this.invocation.source().sendMessage(MiniMessage.miniMessage().deserialize(message));
  }

  @Override
  public final boolean hasPermission(final String permission) {
    return this.invocation.source().hasPermission(permission);
  }

  @Override
  public final String[] getArgs() {
    return this.invocation.arguments();
  }
}
