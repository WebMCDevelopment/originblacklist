package xyz.webmc.originblacklist.core.events;

import xyz.webmc.originblacklist.core.enums.EnumConnectionType;
import xyz.webmc.originblacklist.core.util.OPlayer;

import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftMOTDEvent;

@SuppressWarnings({ "rawtypes" })
public final class OriginBlacklistMOTDEvent extends OriginBlacklistEvent {
  public OriginBlacklistMOTDEvent(final IEaglercraftMOTDEvent eaglerEvent, final Object javaEvent,
      final EnumConnectionType connectionType, final OPlayer player) {
    super(eaglerEvent, javaEvent, connectionType, player);
  }

  @Override
  public final IEaglercraftMOTDEvent getEaglerEvent() {
    return (IEaglercraftMOTDEvent) super.getEaglerEvent();
  }
}