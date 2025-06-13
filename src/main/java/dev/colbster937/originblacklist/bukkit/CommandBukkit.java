package dev.colbster937.originblacklist.bukkit;

import dev.colbster937.originblacklist.base.Command.CommandContext;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandBukkit implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        dev.colbster937.originblacklist.base.Command.handle(new CommandContext() {
            @Override
            public String getName() {
                return sender.getName();
            }

            @Override
            public void reply(String msg) {
                sender.sendMessage(LegacyComponentSerializer.legacySection()
                        .serialize(MiniMessage.miniMessage().deserialize(msg)));
            }

            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }

            @Override
            public String[] getArgs() {
                return args;
            }
        });
        return true;
    }
}