package dev.colbster937.originblacklist.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.colbster937.originblacklist.base.Base;
import net.lax1dude.eaglercraft.backend.server.api.velocity.EaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.velocity.event.EaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.velocity.event.EaglercraftMOTDEvent;
import org.slf4j.Logger;

public class OriginBlacklistVelocity {

    private final ProxyServer proxy;
    private final Base.LoggerAdapter logger;

    @Inject
    public OriginBlacklistVelocity(ProxyServer proxy1, Logger logger1) {
        this.proxy = proxy1;
        this.logger = new Base.LoggerAdapter() {
            @Override public void info(String msg)  { logger1.info(msg); }
            @Override public void warn(String msg)  { logger1.warn(msg); }
            @Override public void error(String msg) { logger1.error(msg); }
        };
        Base.setLogger(this.logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Base.setApi(EaglerXServerAPI.instance());
        Base.reloadConfig();
        Base.init();
        proxy.getCommandManager().register("originblacklist", new CommandVelocity());
        logger.info("Loaded Velocity plugin");
    }

    @Subscribe
    public void onLogin(EaglercraftLoginEvent event) {
        Base.handleConnection(event);
    }

    @Subscribe
    public void onMOTD(EaglercraftMOTDEvent event) {
        Base.handleMOTD(event);
    }
}
