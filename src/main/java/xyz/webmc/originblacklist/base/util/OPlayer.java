package xyz.webmc.originblacklist.base.util;

import xyz.webmc.originblacklist.base.OriginBlacklist;

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

  public OPlayer(final IEaglerConnection conn, final String name, final UUID uuid, final String addr,
      final String brand) {
    this.name = name;
    this.uuid = uuid;
    if (conn != null) {
      this.origin = conn.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
      this.addr = formatSocketAddress(conn.getSocketAddress());
      if (conn instanceof IEaglerLoginConnection) {
        this.brand = ((IEaglerLoginConnection) conn).getEaglerBrandString();
      } else {
        this.brand = OriginBlacklist.UNKNOWN_STR;
      }
    } else {
      this.origin = OriginBlacklist.UNKNOWN_STR;
      this.addr = formatIPAddress(addr);
      this.brand = brand;
    }
  }

  public OPlayer(final IEaglerConnection conn, final String name, final UUID uuid) {
    this(conn, name, uuid, null, null);
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

  private static final String formatIPAddress(String addr) {
    if (addr.startsWith("/")) {
      addr = addr.substring(1);
    }

    int i = addr.lastIndexOf('/');
    if (i != -1) {
      addr = addr.substring(i + 1);
    }

    if (addr.startsWith("[")) {
      i = addr.indexOf(']');
      if (i != -1)
        return addr.substring(1, i);
      return addr.substring(1);
    }

    i = addr.lastIndexOf(':');
    if (i != -1) {
      addr = addr.substring(0, i);
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
