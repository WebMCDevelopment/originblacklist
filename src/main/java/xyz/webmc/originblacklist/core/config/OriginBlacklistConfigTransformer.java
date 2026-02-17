package xyz.webmc.originblacklist.core.config;

import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;

public final class OriginBlacklistConfigTransformer {
  public static final Json5Object transformConfig(final Json5Object config) {
    final Json5Object obj = config.deepCopy();
    final int ver = obj.get("config_version").getAsInt();
    if (ver <= 1) {
      final Json5Object mObj = obj.get("messages").getAsJson5Object().deepCopy();
      final Json5Array motd = mObj.get("motd").getAsJson5Array().deepCopy();
      mObj.remove("motd");
      final Json5Object nObj = new Json5Object();
      nObj.add("text", motd);
      obj.remove("messages");
      obj.add("messages", mObj);
      obj.add("motd", nObj);
      final Json5Object dObj = obj.get("discord").getAsJson5Object().deepCopy();
      final Json5Primitive wEnabled = dObj.get("enabled").getAsJson5Primitive();
      final Json5Array wURLS = dObj.get("webhook_urls").getAsJson5Array().deepCopy();
      final Json5Primitive wAddrs = dObj.get("send_ips").getAsJson5Primitive();
      dObj.remove("enabled");
      dObj.remove("webhook_urls");
      dObj.remove("send_ips");
      final Json5Object oObj = new Json5Object();
      oObj.add("enabled", wEnabled);
      oObj.add("webhook_urls", wURLS);
      oObj.add("send_ips", wAddrs);
      dObj.add("webhook", oObj);
      obj.remove("discord");
      obj.add("discord", dObj);
    }
    obj.remove("config_version");
    obj.add("config_version", Json5Primitive.fromNumber(OriginBlacklistConfig.LATEST_CONFIG_VERSION));
    return obj;
  }
}
