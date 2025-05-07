package dev.colbster937.originblacklist.bungee;

import dev.colbster937.originblacklist.base.Command;
import dev.colbster937.originblacklist.base.Command.CommandContext;
import net.md_5.bungee.api.CommandSender;

public class CommandBungee extends net.md_5.bungee.api.plugin.Command {

    public CommandBungee() {
        super("originblacklist", "originblacklist.use");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Command.handle(new CommandContext() {
            public String getName() { return sender.getName(); }
            public void reply(String msg) { sender.sendMessage(msg); }
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }
            public String[] getArgs() {
                return args;
            }
        });
    }
}
