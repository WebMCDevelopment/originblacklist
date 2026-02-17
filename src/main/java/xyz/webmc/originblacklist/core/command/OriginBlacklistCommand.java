package xyz.webmc.originblacklist.core.command;

import xyz.webmc.originblacklist.core.OriginBlacklist;
import xyz.webmc.originblacklist.core.config.OriginBlacklistConfig;
import xyz.webmc.originblacklist.core.enums.EnumBlacklistType;
import xyz.webmc.originblacklist.core.util.OPlayer;

import java.util.List;

import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Primitive;

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
        final OriginBlacklistConfig config = this.plugin.getConfig();
        final String command = args[0];
        final String argA = args.length > 1 ? args[1] : null;
        ;
        final String argB = args.length > 2 ? args[2] : null;
        final boolean add = "add".equalsIgnoreCase(command);
        final boolean remove = "remove".equalsIgnoreCase(command);
        if ("reload".equalsIgnoreCase(command)) {
          if (ctx.hasPermission("originblacklist.command.reload")) {
            config.reloadConfig();
            this.plugin.handleReload();
            ctx.reply("<green>Configuration Reloaded</green>");
          } else {
            ctx.reply(NO_PERMISSION);
          }
        } else if ("update".equalsIgnoreCase(command)) {
          if (ctx.hasPermission("originblacklist.command.update")) {
            ctx.reply("<aqua>Checking for updates...</aqua>");
            this.plugin.checkForUpdates(() -> {
              ctx.reply("<yellow>Updating plugin...</yellow>");
              this.plugin.updatePlugin(() -> {
                ctx.reply("<green>Successfully updated plugin.</green>");
              }, () -> {
                ctx.reply("<red>Failed to update plugin.</red>");
              });
            }, () -> {
              ctx.reply("<green>Plugin is up to date.</green>");
            });
          } else {
            ctx.reply(NO_PERMISSION);
          }
        } else if ((add || remove) && OriginBlacklist.isNonNullStr(argB)) {
          if ((add && ctx.hasPermission("originblacklist.command.add"))
              || (remove && ctx.hasPermission("originblacklist.command.add"))) {
            final String arrName;
            if ("origin".equalsIgnoreCase(argA)) {
              arrName = "origins";
            } else if ("brand".equalsIgnoreCase(argA)) {
              arrName = "brands";
            } else if ("name".equalsIgnoreCase(argA)) {
              arrName = "player_names";
            } else if ("ip".equalsIgnoreCase(argA)) {
              arrName = "ip_addresses";
            } else {
              arrName = null;
            }
            if (OriginBlacklist.isNonNullStr(arrName)) {
              final String arrPath = "blacklist." + arrName;
              final Json5Array arr = config.getArray(arrPath);
              if (add) {
                if (!arr.contains(Json5Primitive.fromString(argB))) {
                  arr.add(argB);
                  config.set(arrPath, arr);
                  ctx.reply("<green>Added " + argB + " to the " + argA + " blacklist</green>");
                } else {
                  ctx.reply("<red>" + argB + " is already on the " + argA + " blacklist</red>");
                }
              } else if (remove) {
                if (arr.contains(Json5Primitive.fromString(argB))) {
                  arr.remove(Json5Primitive.fromString(argB));
                  config.set(arrPath, arr);
                  ctx.reply("<green>Removed " + argB + " from the " + argA + " blacklist</green>");
                } else {
                  ctx.reply("<red>" + argB + " not on the " + argA + " blacklist</red>");
                }
              }
            } else {
              this.usage(ctx);
            }
          } else {
            ctx.reply(NO_PERMISSION);
          }
        } else if ("test".equalsIgnoreCase(command) && OriginBlacklist.isNonNullStr(argA)) {
          if (ctx.hasPermission("originblacklist.command.test")) {
            if (this.isBlacklisted(argA)) {
              ctx.reply("<green>" + argA + " is on the blacklist.</green>");
            } else {
              ctx.reply("<red>" + argA + " is not on the blacklist.</red>");
            }
          } else {
            ctx.reply(NO_PERMISSION);
          }
        } else if ("list".equalsIgnoreCase(command)) {
          if (ctx.hasPermission("originblacklist.command.list")) {
            ctx.reply("<aqua>Blacklist:</aqua>");
            ctx.reply("<gold>  - Origins:</gold>");
            for (final Json5Element element : config.getArray("blacklist.origins")) {
              ctx.reply("<gray>    - " + element.getAsString() + "</gray>");
            }
            ctx.reply("<gold>  - Brands:</gold>");
            for (final Json5Element element : config.getArray("blacklist.brands")) {
              ctx.reply("<gray>    - " + element.getAsString() + "</gray>");
            }
            ctx.reply("<gold>  - Players:</gold>");
            for (final Json5Element element : config.getArray("blacklist.player_names")) {
              ctx.reply("<gray>    - " + element.getAsString() + "</gray>");
            }
            ctx.reply("<gold>  - IPs:</gold>");
            for (final Json5Element element : config.getArray("blacklist.ip_addresses")) {
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
  public final List<String> suggest(final CommandContext ctx) {
    return List.of();
  }

  @Override
  public final void usage(CommandContext ctx) {
    ctx.reply("<aqua>Commands:</aqua>");
    ctx.reply("<gray>  - /originblacklist reload</gray>");
    ctx.reply("<gray>  - /originblacklist update</gray>");
    ctx.reply("<gray>  - /originblacklist add <origin/brand/name/ip> <arg></gray>");
    ctx.reply("<gray>  - /originblacklist remove <origin/brand/name/ip> <arg></gray>");
    ctx.reply("<gray>  - /originblacklist test <arg></gray>");
    ctx.reply("<gray>  - /originblacklist list</gray>");
  }

  private final boolean isBlacklisted(final String str) {
    final OPlayer player = new OPlayer(null, str, null, str, str, null, -1);
    return this.plugin.testBlacklist(player) != EnumBlacklistType.NONE;
  }
}
