package xyz.webmc.originblacklist.core.events;

import xyz.webmc.originblacklist.core.enums.EnumConnectionType;
import xyz.webmc.originblacklist.core.util.OPlayer;

import net.lax1dude.eaglercraft.backend.server.api.event.IBaseServerEvent;

@SuppressWarnings({ "rawtypes" })
public abstract class OriginBlacklistEvent {
  private final EnumConnectionType connectionType;
  private final IBaseServerEvent eaglerEvent;
  private final Object javaEvent;
  private final OPlayer player;

  protected OriginBlacklistEvent(final IBaseServerEvent eaglerEvent, final Object javaEvent, final EnumConnectionType connectionType, final OPlayer player) {
    this.eaglerEvent = eaglerEvent;
    this.javaEvent = javaEvent;
    this.connectionType = connectionType;
    this.player = player;
  }

  protected IBaseServerEvent getEaglerEvent() {
    return this.eaglerEvent;
  }

  public final Object getJavaEvent() {
    return this.javaEvent;
  }

  public final EnumConnectionType getConnectionType() {
    return this.connectionType;
  }

  public final OPlayer getPlayer() {
    return this.player;
  }
}