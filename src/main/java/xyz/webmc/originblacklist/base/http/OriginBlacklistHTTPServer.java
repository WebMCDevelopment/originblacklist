package xyz.webmc.originblacklist.base.http;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.config.OriginBlacklistConfig;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class OriginBlacklistHTTPServer {
  private final OriginBlacklist plugin;
  private final ExecutorService executor;
  private HttpServer server;
  private boolean running;

  public OriginBlacklistHTTPServer(final OriginBlacklist plugin) {
    this.plugin = plugin;
    this.executor = Executors.newFixedThreadPool(4);
    this.running = false;
  }

  public final void start() {
    if (!this.running) {
      try {
        this.server = this.createServer();
        this.server.start();
        this.running = true;
      } catch (final Throwable t) {
      }
    }
  }

  public final void stop() {
    if (this.running && this.server != null) {
      this.server.stop(0);
      this.running = false;
    }
  }

  public final void shutdown() {
    this.stop();
    this.executor.shutdownNow();
  }

  private final HttpServer createServer() throws Throwable {
    final OriginBlacklistConfig config = this.plugin.getConfig();
    final HttpServer server = HttpServer
        .create(new InetSocketAddress(config.getString("blacklist_http_share.listen_addr"),
            config.getInteger("blacklist_http_share.http_port")), 0);
    server.createContext("/", new OriginBlacklistHTTPHandler(this.plugin));
    server.setExecutor(executor);
    return server;
  }
}
