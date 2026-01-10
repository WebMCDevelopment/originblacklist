package xyz.webmc.originblacklist.base.command;

import xyz.webmc.originblacklist.base.OriginBlacklist;

import java.util.List;

import de.marhali.json5.Json5Element;

public class OriginBlacklistCommand implements ICommand {
  private final OriginBlacklist plugin;

  public OriginBlacklistCommand(OriginBlacklist plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean execute(final CommandContext ctx) {
    final String[] args = ctx.getArgs();
    if (ctx.hasPermission("originblacklist.command")) {
      if (args.length > 0) {
        final String command = args[0].toLowerCase();
        if ("reload".equals(command)) {
          if (ctx.hasPermission("originblacklist.command.reload")) {
            this.plugin.getConfig().reloadConfig();
            ctx.reply("<green>Configuration Reloaded</green>");
          } else {
            ctx.reply(NO_PERMISSION);
          }
        } else if ("list".equals(command)) {
          if (ctx.hasPermission("originblacklist.command.reload")) {
            ctx.reply("<aqua>Blacklist:</aqua>");
            ctx.reply("<gold>  - Origins:</gold>");
            for (final Json5Element element : this.plugin.getConfig().get("blacklist.origins").getAsJson5Array()) {
              ctx.reply("<gray>    - " + element.getAsString() + "</gray>");
            }
            ctx.reply("<gold>  - Brands:</gold>");
            for (final Json5Element element : this.plugin.getConfig().get("blacklist.brands").getAsJson5Array()) {
              ctx.reply("<gray>    - " + element.getAsString() + "</gray>");
            }
            ctx.reply("<gold>  - Players:</gold>");
            for (final Json5Element element : this.plugin.getConfig().get("blacklist.player_names").getAsJson5Array()) {
              ctx.reply("<gray>    - " + element.getAsString() + "</gray>");
            }
            ctx.reply("<gold>  - IPs:</gold>");
            for (final Json5Element element : this.plugin.getConfig().get("blacklist.ip_addresses").getAsJson5Array()) {
              ctx.reply("<gray>    - " + element.getAsString() + "</gray>");
            }
          } else {
            ctx.reply(NO_PERMISSION);
          }
        } else {
          this.usage(ctx);
        }
      } else {
        this.usage(ctx);
      }
    } else {
      ctx.reply("");
    }
    return true;
  }

  @Override
  public List<String> suggest(final CommandContext ctx) {
    return List.of();
  }

  @Override
  public void usage(CommandContext ctx) {
    ctx.reply("<aqua>Commands:</aqua>");
    ctx.reply("<gray>  - /originblacklist reload</gray>");
    //ctx.reply("<gray>  - /originblacklist add <brand/origin/name/ip> <value></gray>");
    //ctx.reply("<gray>  - /originblacklist remove <brand/origin/name/ip> <value></gray>");
    ctx.reply("<gray>  - /originblacklist list</gray>");
  }
}
