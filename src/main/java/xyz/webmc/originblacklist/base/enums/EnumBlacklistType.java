package xyz.webmc.originblacklist.base.enums;

import xyz.webmc.originblacklist.base.OriginBlacklist;

public enum EnumBlacklistType {
  ORIGIN("origin", "website", "origins", null),
  BRAND("brand", "client", "brands", null),
  NAME("name", "username", "player_names", "player_name"),
  ADDR("addr", "ip address", "ip_addresses", "ip_address"),
  NONE(null, null, null, null);

  private final String str;
  private final String alt;
  private final String arr;
  private final String act;

  private EnumBlacklistType(final String str, final String alt, final String arr, final String act) {
    this.str = str;
    this.alt = alt;
    this.arr = arr;
    this.act = OriginBlacklist.isNonNull(act) ? act : OriginBlacklist.GENERIC_STR;
  }

  public final String getString() {
    return this.str;
  }

  public final String getAltString() {
    return this.alt;
  }

  public final String getArrayString() {
    return this.arr;
  }

  public final String getActionString() {
    return this.act;
  }
}
