package xyz.webmc.originblacklist.core.util;

import xyz.webmc.originblacklist.core.OriginBlacklist;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

import net.lax1dude.eaglercraft.backend.server.api.EnumWebSocketHeader;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerConnection;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerLoginConnection;

public final class OPlayer {
  private final String origin;
  private final String addr;
  private final String name;
  private final UUID uuid;
  private final String brand;
  private final String vhost;
  private final boolean rewind;
  private final int pvn;

  public OPlayer(final IEaglerConnection conn, final String name, final UUID uuid, final String addr,
      final String brand, final String vhost, final int pvn) {
    this.name = name;
    this.uuid = uuid;
    if (conn != null) {
      this.origin = conn.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
      this.addr = formatSocketAddress(conn.getSocketAddress());
      this.vhost = conn.isWebSocketSecure() ? "wss://" : "ws://" + conn.getWebSocketHost();
      if (conn instanceof IEaglerLoginConnection) {
        final IEaglerLoginConnection loginConn = (IEaglerLoginConnection) conn;
        this.brand = loginConn.getEaglerBrandString();
        this.rewind = loginConn.isEaglerXRewindPlayer();
        this.pvn = this.rewind ? loginConn.getRewindProtocolVersion() : loginConn.getMinecraftProtocol();
      } else {
        this.brand = OriginBlacklist.UNKNOWN_STR;
        this.rewind = false;
        this.pvn = pvn;
      }
    } else {
      this.origin = OriginBlacklist.UNKNOWN_STR;
      this.addr = formatIPAddress(addr);
      this.brand = brand;
      this.vhost = vhost;
      this.rewind = false;
      this.pvn = pvn;
    }
  }

  public OPlayer(final IEaglerConnection conn, final String name, final UUID uuid) {
    this(conn, name, uuid, null, null, null, -1);
  }

  public final String getOrigin() {
    return this.origin;
  }

  public final String getAddr() {
    return this.addr;
  }

  public final String getName() {
    return this.name;
  }

  public final UUID getUUID() {
    return this.uuid;
  }

  public final String getBrand() {
    return this.brand;
  }

  public final String getVHost() {
    return this.vhost;
  }

  public final boolean isRewind() {
    return this.rewind;
  }

  public final int getPVN() {
    return this.pvn;
  }

  private static final String formatIPAddress(String addr) {
    if (OriginBlacklist.isNonNullStr(addr)) {
      if (addr.startsWith("/")) {
        addr = addr.substring(1);
      }

      int i = addr.lastIndexOf('/');
      if (i != -1) {
        addr = addr.substring(i + 1);
      }

      if (addr.startsWith("[")) {
        i = addr.indexOf(']');
        if (i != -1) {
          addr = addr.substring(1, i);
        } else {
          addr = addr.substring(1);
        }
      } else {
        i = addr.lastIndexOf(':');
        if (i != -1) {
          String a = addr.substring(0, i);
          String p = addr.substring(i + 1);

          boolean port = !p.isEmpty();
          for (int j = 0; j < p.length() && port; j++) {
            char c = p.charAt(j);
            port = (c >= '0' && c <= '9');
          }

          if (port) {
            if (a.indexOf('.') != -1) {
              addr = a;
            }
          }
        }
      }

      int c = 0;
      boolean hex = true;
      for (int j = 0; j < addr.length(); j++) {
        char ch = addr.charAt(j);
        if (ch == ':') {
          c++;
        } else if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))) {
          hex = false;
          break;
        }
      }

      if (hex && c == 6 && addr.indexOf("::") == -1) {
        addr = addr + "::";
      }
    } else {
      addr = OriginBlacklist.UNKNOWN_STR;
    }

    return addr;
  }

  private static final String formatSocketAddress(final SocketAddress saddr) {
    if (saddr instanceof InetSocketAddress) {
      final InetSocketAddress isa = (InetSocketAddress) saddr;
      if (isa.getAddress() != null) {
        return isa.getAddress().getHostAddress();
      } else {
        return isa.getHostString();
      }
    } else {
      return formatIPAddress(saddr.toString());
    }
  }
}
