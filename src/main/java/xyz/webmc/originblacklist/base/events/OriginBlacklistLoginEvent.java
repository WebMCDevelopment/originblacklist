package xyz.webmc.originblacklist.base.events;

import xyz.webmc.originblacklist.base.enums.EnumConnectionType;
import xyz.webmc.originblacklist.base.util.OPlayer;

import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftLoginEvent;

@SuppressWarnings({ "rawtypes" })
public final class OriginBlacklistLoginEvent extends OriginBlacklistEvent {
  public OriginBlacklistLoginEvent(final IEaglercraftLoginEvent eaglerEvent, final Object javaEvent,
      final EnumConnectionType connectionType, final OPlayer player) {
    super(eaglerEvent, javaEvent, connectionType, player);
  }

  @Override
  public final IEaglercraftLoginEvent getEaglerEvent() {
    return (IEaglercraftLoginEvent) super.getEaglerEvent();
  }
}
