package dev.colbster937.originblacklist.bukkit;

import dev.colbster937.originblacklist.base.Base;
import net.lax1dude.eaglercraft.backend.server.api.bukkit.EaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.bukkit.event.EaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.bukkit.event.EaglercraftMOTDEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

public class OriginBlacklistBukkit extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Plugin plugin = getServer().getPluginManager().getPlugin("EaglercraftXServer");
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

        getCommand("originblacklist").setExecutor(new CommandBukkit());
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Loaded Bukkit plugin");
    }

    @EventHandler
    public void onLogin(EaglercraftLoginEvent event) {
        Base.handleConnection(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMOTD(EaglercraftMOTDEvent event) {
        Base.handleMOTD(event);
    }
}
