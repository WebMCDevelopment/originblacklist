package dev.colbster937.originblacklist.base;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static dev.colbster937.originblacklist.base.Base.config;

public class Command {
    private static final String permission = "<red>You do not have permission to use this command.</red>";

    public interface CommandContext {
        String getName();
        void reply(String message);
        boolean hasPermission(String permission);
        String[] getArgs();
    }

    public static void usage(CommandContext ctx) {
        ctx.reply("<aqua>Commands:</aqua>");
        ctx.reply("<gray>  - /originblacklist reload</gray>");
        ctx.reply("<gray>  - /originblacklist add <brand/origin/name/ip> <value></gray>");
        ctx.reply("<gray>  - /originblacklist remove <brand/origin/name/ip> <value></gray>");
        ctx.reply("<gray>  - /originblacklist list</gray>");
    }

    public static void handle(CommandContext ctx) {
        String[] args = ctx.getArgs();
        if (!ctx.hasPermission("originblacklist.command")) {
            ctx.reply(permission);
            return;
        } else if (args.length == 0) {
            usage(ctx);
            return;
        }

        String sub = args[0].toLowerCase();
        String sub1 = args.length > 1 ? args[1].toLowerCase() : "";
        String sub2 = args.length > 2 ? args[2].toLowerCase() : "";

        switch (sub) {
            case "reload" -> {
                if (ctx.hasPermission("originblacklist.reload")) {
                    Base.reloadConfig();
                    ctx.reply("<green>Reloaded.</green>");
                } else {
                    ctx.reply(permission);
                    return;
                }
            }

            case "add" -> {
                if (!ctx.hasPermission("originblacklist.add")) {
                    ctx.reply(permission);
                    return;
                }

                if (sub1.isEmpty() || sub2.isEmpty()) {
                    usage(ctx);
                    return;
                }

                List<String> list = switch (sub1) {
                    case "brand" -> Base.config.blacklist.brands;
                    case "origin" -> Base.config.blacklist.origins;
                    case "player" -> Base.config.blacklist.players;
                    case "ip" -> Base.config.blacklist.ips;
                    default -> null;
                };

                if (list == null) {
                    usage(ctx);
                    return;
                }

                if (!list.contains(sub2)) {
                    list.add(sub2);
                    ctx.reply("<green>Added " + sub2 + " to " + sub1 + " blacklist.</green>");
                } else {
                    ctx.reply("<yellow>Already blacklisted.</yellow>");
                }
                try {
                    config.saveConfig(Base.config.toMap(), new File("plugins/originblacklist/config.yml"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Base.reloadConfig();
            }

            case "remove" -> {
                if (!ctx.hasPermission("originblacklist.remove")) {
                    ctx.reply(permission);
                    return;
                }

                if (sub1.isEmpty() || sub2.isEmpty()) {
                    usage(ctx);
                    return;
                }

                List<String> list = switch (sub1) {
                    case "brand" -> Base.config.blacklist.brands;
                    case "origin" -> Base.config.blacklist.origins;
                    case "player" -> Base.config.blacklist.players;
                    case "ip" -> Base.config.blacklist.ips;
                    default -> null;
                };

                if (list == null) {
                    usage(ctx);
                    return;
                }

                if (list.remove(sub2)) {
                    ctx.reply("<green>Removed " + sub2 + " from " + sub1 + " blacklist.</green>");
                } else {
                    ctx.reply("<yellow>Entry not found in " + sub1 + ".</yellow>");
                }
                try {
                    config.saveConfig(Base.config.toMap(), new File("plugins/originblacklist/config.yml"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Base.reloadConfig();
            }

            case "list" -> {
                if (!ctx.hasPermission("originblacklist.view")) {
                    ctx.reply(permission);
                    return;
                }

                ctx.reply("<aqua>Blacklist:</aqua>");

                ctx.reply("<gray>  Brands:</gray>");
                for (String s : Base.config.blacklist.brands) ctx.reply("<gray>   - " + s + "</gray>");

                ctx.reply("<gray>  Origins:</gray>");
                for (String s : Base.config.blacklist.origins) ctx.reply("<gray>   - " + s + "</gray>");

                ctx.reply("<gray>  Players:</gray>");
                for (String s : Base.config.blacklist.players) ctx.reply("<gray>   - " + s + "</gray>");

                ctx.reply("<gray>  IPs:</gray>");
                for (String s : Base.config.blacklist.ips) ctx.reply("<gray>   - " + s + "</gray>");
            }

            default -> usage(ctx);
        }
    }

    public static List<String> suggest(CommandContext ctx) {
        String[] args = ctx.getArgs();

        if (args.length == 1) {
            return List.of("reload", "add", "remove", "list");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return List.of("brand", "origin", "player", "ip");
        }

        return List.of();
    }

    public Map<String, Object> toMap() {
        Yaml yaml = new Yaml();
        return yaml.load(yaml.dump(this));
    }
}
