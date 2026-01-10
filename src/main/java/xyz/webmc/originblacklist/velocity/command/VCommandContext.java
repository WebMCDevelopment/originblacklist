package xyz.webmc.originblacklist.velocity.command;

import xyz.webmc.originblacklist.base.command.CommandContext;

import com.velocitypowered.api.command.SimpleCommand.Invocation;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class VCommandContext implements CommandContext {
  private final Invocation invocation;

  public VCommandContext(final Invocation invocation) {
    this.invocation = invocation;
  }

  @Override
  public String getPlayerName() {
    return this.invocation.source().toString();
  }

  @Override
  public void reply(final String message) {
    this.invocation.source().sendMessage(MiniMessage.miniMessage().deserialize(message)); 
  }

  @Override
  public boolean hasPermission(final String permission) {
    return this.invocation.source().hasPermission(permission);
  }

  @Override
  public String[] getArgs() {
    return this.invocation.arguments();
  }
}
