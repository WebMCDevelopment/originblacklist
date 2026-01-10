package xyz.webmc.originblacklist.velocity.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.command.OriginBlacklistCommand;

import java.util.List;

import com.velocitypowered.api.command.SimpleCommand;

public class OriginBlacklistCommandVelocity extends OriginBlacklistCommand implements SimpleCommand {
  public OriginBlacklistCommandVelocity(OriginBlacklist plugin) {
    super(plugin);
  }

  @Override
  public void execute(final Invocation invocation) {
    super.execute(new VCommandContext(invocation));
  }

  @Override
  public List<String> suggest(final Invocation invocation) {
    return super.suggest(new VCommandContext(invocation));
  }
}
