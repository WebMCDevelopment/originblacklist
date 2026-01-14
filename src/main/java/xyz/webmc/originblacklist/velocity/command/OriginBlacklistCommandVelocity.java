package xyz.webmc.originblacklist.velocity.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.command.OriginBlacklistCommand;

import java.util.List;

import com.velocitypowered.api.command.SimpleCommand;

public final class OriginBlacklistCommandVelocity extends OriginBlacklistCommand implements SimpleCommand {
  private final OriginBlacklist plugin;

  public OriginBlacklistCommandVelocity(OriginBlacklist plugin) {
    super(plugin);
    this.plugin = plugin;
  }

  @Override
  public final void execute(final Invocation invocation) {
    super.execute(new VCommandContext(this.plugin, invocation));
  }

  @Override
  public final List<String> suggest(final Invocation invocation) {
    return super.suggest(new VCommandContext(this.plugin, invocation));
  }
}
