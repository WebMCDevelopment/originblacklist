package dev.colbster937.originblacklist.base;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPlayer;
import net.lax1dude.eaglercraft.backend.server.api.EnumWebSocketHeader;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftInitializePlayerEvent;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Base {
    private static LoggerAdapter adapter;
    private static IEaglerXServerAPI api;
    private static ConfigManager config;

    public static void setLogger(LoggerAdapter log) {
        adapter = log;
    }

    public static void setApi(IEaglerXServerAPI api1) {
        api = api1;
    }

    public static LoggerAdapter getLogger() {
        if (adapter == null) throw new IllegalStateException("Logger not initialized!");
        return adapter;
    }

    public interface LoggerAdapter {
        void info(String msg);
        void warn(String msg);
        void error(String msg);
    }

    public static void handleConnection(IEaglercraftInitializePlayerEvent e) {
        IEaglerPlayer plr = e.getPlayer();
        String origin = plr.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
        String brand = plr.getEaglerBrandString();
        if ((origin != "null" || origin != null) && !config.blacklist.missing_origin) {
            for (String origin1 : config.blacklist.origins) {
                if (matches(origin, origin1)) {
                    plr.disconnect(kick("origin", "website", origin));
                    webhook(plr, origin, brand, "origin");
                    return;
                }
            }
        } else {
            plr.disconnect(kick("origin", "website", origin));
            webhook(plr, "null", brand, "origin");
        }
        if (brand != "null" && brand != null) {
            for (String brand1 : config.blacklist.brands) {
                if (matches(brand, brand1)) {
                    plr.disconnect(kick("brand", "client", brand));
                    webhook(plr, origin, brand, "brand");
                    return;
                }
            }
        }
    }

    public static boolean matches(String text1, String text2) {
        return text1.toLowerCase().matches(text2.replace(".", "\\.").replaceAll("\\*", ".*").toLowerCase());
    }

    public static String kick(String type, String easytype, String value) {
        return LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage().deserialize(
                        config.messages.kick
                                .replace("%blocktype%", type)
                                .replace("%easyblocktype%", easytype)
                                .replace("%blocked%", value)
                )
        );
    }


    public static void webhook(IEaglerPlayer plr, String origin, String brand, String type) {
        String webhook = config.discord.webhook;
        if (webhook == null || webhook.isBlank()) return;

        String addr = plr.getPlayerAddress() != null ? plr.getPlayerAddress().toString().substring(1) : "undefined";
        String protocol = plr.isEaglerXRewindPlayer()
                ? (String.valueOf(plr.getRewindProtocolVersion()) != null ? String.valueOf(plr.getRewindProtocolVersion()) : "undefined")
                : (String.valueOf(plr.getMinecraftProtocol()) != null ? String.valueOf(plr.getMinecraftProtocol()) : "undefined");
        String rewind = plr.isEaglerXRewindPlayer() ? "Yes" : "No";
        String userAgent = plr.getWebSocketHeader(EnumWebSocketHeader.HEADER_USER_AGENT);
        if (userAgent == null || userAgent.isEmpty()) userAgent = "undefined";

        String payload = String.format("""
        {
          "content": "Blocked a blacklisted %s from joining",
          "embeds": [
            {
              "title": "Player Information",
              "description": "🎮 **Name:** %s\\n🏠 **Address:** %s\\n🌄 **PVN:** %s\\n🌐 **Origin:** %s\\n🔋 **Brand:** %s\\n🧊 **User Agent:** %s\\n⏪ **Rewind:** %s"
            }
          ]
        }
        """, type, plr.getUsername(), addr, protocol, origin, brand, userAgent, rewind);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(webhook).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
            }

            conn.getInputStream().close();
        } catch (Exception e) {
            getLogger().warn("Failed to send webhook: " + e);
        }
    }

    public static void reloadConfig() {
        config = ConfigManager.loadConfig(adapter);
    }
}
