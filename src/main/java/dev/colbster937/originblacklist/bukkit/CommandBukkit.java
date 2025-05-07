package dev.colbster937.originblacklist.bukkit;

import dev.colbster937.originblacklist.base.Command;
import dev.colbster937.originblacklist.base.Command.CommandContext;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

public class CommandBukkit implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
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
        return true;
    }
}
