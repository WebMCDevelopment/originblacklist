package xyz.webmc.originblacklist.base.http;

import xyz.webmc.originblacklist.base.OriginBlacklist;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class OriginBlacklistHTTPHandler implements HttpHandler {
  private final OriginBlacklist plugin;

  public OriginBlacklistHTTPHandler(final OriginBlacklist plugin) {
    this.plugin = plugin; 
  }

  @Override
  public final void handle(final HttpExchange exchange) throws IOException {
    try {
      final String path = exchange.getRequestURI().getPath();
      if ("/".equals(path)) {
        final byte[] bytes = this.plugin.getBlacklistShare().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);

        try (final OutputStream os = exchange.getResponseBody()) {
          os.write(bytes);
        }
      } else {
        exchange.sendResponseHeaders(404, -1);
      }
    } finally {
      exchange.close();
    }
  }
}
