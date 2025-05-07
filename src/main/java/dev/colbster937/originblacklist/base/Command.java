package dev.colbster937.originblacklist.base;

public class Command {

    public interface CommandContext {
        String getName();
        void reply(String message);
        boolean hasPermission(String permission);
        String[] getArgs();
    }


    public static void usage(CommandContext ctx) {
        ctx.reply("<aqua>Commands:</aqua>");
        ctx.reply("<gray>  - /originblacklist reload</gray>");
    }

    public static void handle(CommandContext ctx) {
        String[] args = ctx.getArgs();
        if (!ctx.hasPermission("originblacklist.reload")) {
            ctx.reply("<red>You do not have permission to use this command.</red>");
            return;
        } else if (args.length == 0) {
            usage(ctx);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                Base.reloadConfig();
                ctx.reply("<green>Reloaded.</green>");
            }

            default -> usage(ctx);
        }
    }
}
