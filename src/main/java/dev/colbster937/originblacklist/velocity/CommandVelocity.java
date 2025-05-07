package dev.colbster937.originblacklist.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import dev.colbster937.originblacklist.base.Command.CommandContext;
import net.kyori.adventure.text.minimessage.MiniMessage;
import com.velocitypowered.api.command.CommandSource;

public class CommandVelocity implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        dev.colbster937.originblacklist.base.Command.handle(new CommandContext() {
            @Override
            public String getName() {
                return source.toString();
            }

            @Override
            public void reply(String msg) {
                source.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            }

            @Override
            public boolean hasPermission(String permission) {
                return source.hasPermission(permission);
            }

            public String[] getArgs() {
                return invocation.arguments();
            }
        });
    }
}
