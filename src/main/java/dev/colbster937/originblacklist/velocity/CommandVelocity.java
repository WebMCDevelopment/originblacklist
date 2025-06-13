package dev.colbster937.originblacklist.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import dev.colbster937.originblacklist.base.Command;
import dev.colbster937.originblacklist.base.Command.CommandContext;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public class CommandVelocity implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        Command.handle(new VelocityCommandContext(invocation));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return Command.suggest(new VelocityCommandContext(invocation));
    }

    public static class VelocityCommandContext implements CommandContext {
        private final Invocation invocation;

        public VelocityCommandContext(Invocation invocation) {
            this.invocation = invocation;
        }

        @Override
        public String getName() {
            return invocation.source().toString();
        }

        @Override
        public void reply(String message) {
            invocation.source().sendMessage(MiniMessage.miniMessage().deserialize(message));
        }

        @Override
        public boolean hasPermission(String permission) {
            return invocation.source().hasPermission(permission);
        }

        @Override
        public String[] getArgs() {
            return invocation.arguments();
        }
    }
}