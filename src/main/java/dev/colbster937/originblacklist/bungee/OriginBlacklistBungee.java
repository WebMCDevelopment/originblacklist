package dev.colbster937.originblacklist.bungee;

import dev.colbster937.originblacklist.base.Base;
import net.lax1dude.eaglercraft.backend.server.api.bungee.EaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.bungee.event.EaglercraftInitializePlayerEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class OriginBlacklistBungee extends Plugin implements Listener {

    @Override
    public void onEnable() {
        Base.setLogger(new Base.LoggerAdapter() {
            @Override public void info(String msg)  { getLogger().info(msg); }
            @Override public void warn(String msg)  { getLogger().warning(msg); }
            @Override public void error(String msg) { getLogger().severe(msg); }
        });

        Base.setApi(EaglerXServerAPI.instance());
        Base.reloadConfig();

        getProxy().getPluginManager().registerCommand(this, new CommandBungee());
        getProxy().getPluginManager().registerListener(this, this);

        getLogger().info("Loaded Bungee plugin");
    }

    @EventHandler
    public void onLogin(EaglercraftInitializePlayerEvent event) {
        Base.handleConnection(event);
    }
}
