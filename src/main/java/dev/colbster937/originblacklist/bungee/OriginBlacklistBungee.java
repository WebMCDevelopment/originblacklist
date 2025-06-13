package dev.colbster937.originblacklist.bungee;

import dev.colbster937.originblacklist.base.Base;
import net.lax1dude.eaglercraft.backend.server.api.bungee.EaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.bungee.event.EaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.bungee.event.EaglercraftMOTDEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class OriginBlacklistBungee extends Plugin implements Listener {

    @Override
    public void onEnable() {
        Plugin plugin = getProxy().getPluginManager().getPlugin("EaglercraftXServer");
        if (plugin != null) {
            String version = plugin.getDescription().getVersion();
            if (!Base.checkVer(version, Base.pluginVer)) {
                getLogger().severe("EaglerXServer " + Base.pluginVer + " is required!");
                throw new RuntimeException("Incompatible plugin version");
            }
        } else {
            throw new RuntimeException("Missing EaglerXServer");
        }


        Base.setLogger(new Base.LoggerAdapter() {
            @Override public void info(String msg)  { getLogger().info(msg); }
            @Override public void warn(String msg)  { getLogger().warning(msg); }
            @Override public void error(String msg) { getLogger().severe(msg); }
        });

        Base.setApi(EaglerXServerAPI.instance());
        Base.reloadConfig();
        Base.init();

        getProxy().getPluginManager().registerCommand(this, new CommandBungee());
        getProxy().getPluginManager().registerListener(this, this);

        getLogger().info("Loaded Bungee plugin");
    }

    @EventHandler
    public void onLogin(EaglercraftLoginEvent event) {
        Base.handleConnection(event);
    }

    @EventHandler
    public void onMOTD(EaglercraftMOTDEvent event) {
        Base.handleMOTD(event);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        String ip = event.getConnection().getAddress().getAddress().getHostAddress();
        String name = event.getConnection().getName();
        String blacklisted = Base.handlePre(ip, name);
        if (!blacklisted.equals("false")) {
            event.setCancelReason(blacklisted);
            event.setCancelled(true);
        }
    }
}
