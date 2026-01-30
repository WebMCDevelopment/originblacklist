package xyz.webmc.originblacklist.base.http;

import xyz.webmc.originblacklist.base.OriginBlacklist;

import java.nio.charset.StandardCharsets;

import net.lax1dude.eaglercraft.backend.server.api.webserver.IRequestContext;
import net.lax1dude.eaglercraft.backend.server.api.webserver.IRequestHandler;
import net.lax1dude.eaglercraft.backend.server.api.webserver.RouteDesc;

public final class OriginBlacklistRequestHandler implements IRequestHandler {
  private static final RouteDesc route = RouteDesc.create("/originblacklist/v2/");
  private final OriginBlacklist plugin;

  public OriginBlacklistRequestHandler(final OriginBlacklist plugin) {
    this.plugin = plugin;
  }

  @Override
  public final void handleRequest(final IRequestContext ctx) {
    final String path = ctx.getPath();
    if (route.getPattern().equals(path)) {
      final byte[] bytes = this.plugin.getBlacklistShare().getBytes(StandardCharsets.UTF_8);

      ctx.addResponseHeader("Content-Type", "application/json; charset=utf-8");
      ctx.setResponseCode(200);
      ctx.setResponseBody(bytes);
    } else {
      ctx.getServer().get404Handler().handleRequest(ctx);
    }
  }

  public static final void register(final OriginBlacklist plugin) {
    plugin.getEaglerAPI().getWebServer().registerRoute(plugin, route, new OriginBlacklistRequestHandler(plugin));
  }

  public static final void unRegister(final OriginBlacklist plugin) {
    plugin.getEaglerAPI().getWebServer().unregisterRoute(plugin, route);
  }
}
