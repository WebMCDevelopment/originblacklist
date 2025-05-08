package dev.colbster937.originblacklist.base;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.*;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftClientBrandEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftMOTDEvent;
import net.lax1dude.eaglercraft.backend.server.api.query.IMOTDConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

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

    public static String apiVer = "1.0.2";

    public static boolean checkVer(String v1, String v2) {
        String[] c = v1.split("\\.");
        String[] r = v2.split("\\.");
        for (int i = 0; i < Math.max(c.length, r.length); i++) {
            int c1 = i < c.length ? Integer.parseInt(c[i]) : 0;
            int r1 = i < r.length ? Integer.parseInt(r[i]) : 0;
            if (c1 < r1) return false;
            if (c1 > r1) return true;
        }
        return true;
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

    public static void handleConnection(IEaglercraftClientBrandEvent e) {
        IEaglerPendingConnection conn = e.getPendingConnection();
        String origin = conn.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
        String brand = conn.getEaglerBrandString();
        if ((origin != "null" || origin != null) && !config.blacklist.missing_origin) {
            for (String origin1 : config.blacklist.origins) {
                if (matches(origin, origin1)) {
                    e.setKickMessage(kick("origin", "website", origin));
                    webhook(conn, origin, brand, "origin");
                    return;
                }
            }
        } else {
            if (origin != "null" || origin != null) {
                e.setKickMessage(kick("origin", "website", origin));
                webhook(conn, "null", brand, "origin");
                return;
            }
        }
        if (brand != "null" && brand != null) {
            for (String brand1 : config.blacklist.brands) {
                if (matches(brand, brand1)) {
                    e.setKickMessage(kick("brand", "client", brand));
                    webhook(conn, origin, brand, "brand");
                    return;
                }
            }
        }
    }

    public static void handleMOTD(IEaglercraftMOTDEvent e) {
        if (config.messages.motd.enabled) {
            IMOTDConnection conn = e.getMOTDConnection();
            String origin = conn.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
            List<String> m = List.of(config.messages.motd.text.split("\n")).stream()
                    .map(line -> line
                            .replace("%blocktype%", "origin")
                            .replace("%easyblocktype%", "website")
                            .replace("%blocked%", origin))
                    .map(line -> LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(
                                    line
                            )
                    )).collect(Collectors.toList());
            if ((origin != "null" || origin != null) && !config.blacklist.missing_origin) {
                for (String origin1 : config.blacklist.origins) {
                    if (matches(origin, origin1)) {
                        setMOTD(conn, m);
                        return;
                    }
                }
            } else {
                if (origin != "null" || origin != null) {
                    setMOTD(conn, m);
                }
            }
        }
    }

    public static void setMOTD(IMOTDConnection conn, List<String> m) {
        conn.setServerMOTD(m);
        conn.setPlayerTotal(0);
        conn.setPlayerMax(0);
        conn.setPlayerList(List.of());
        if (config.messages.motd.icon != null && !config.messages.motd.icon.isEmpty())
            try {
                BufferedImage img = ImageIO.read(new File(config.messages.motd.icon));
                if (img.getWidth() != 64 || img.getHeight() != 64) {
                    getLogger().warn("Icon must be 64x64");
                    return;
                }
                byte[] bytes = new byte[64 * 64 * 4];
                for (int y = 0; y < 64; y++) {
                    for (int x = 0; x < 64; x++) {
                        int pixel = img.getRGB(x, y);
                        int i = (y * 64 + x) * 4;
                        bytes[i] = (byte) ((pixel >> 16) & 0xFF);
                        bytes[i + 1] = (byte) ((pixel >> 8) & 0xFF);
                        bytes[i + 2] = (byte) (pixel & 0xFF);
                        bytes[i + 3] = (byte) ((pixel >> 24) & 0xFF);
                    }
                }
                conn.setServerIcon(bytes);
            } catch (IOException ex) {
                getLogger().error(ex.toString());
            }
    }

    public static boolean matches(String text1, String text2) {
        return text1.toLowerCase().matches(text2.replace(".", "\\.").replaceAll("\\*", ".*").toLowerCase());
    }

    public static Component kick(String type, String easytype, String value) {
        return MiniMessage.miniMessage().deserialize(
                config.messages.kick
                        .replace("%blocktype%", type)
                        .replace("%easyblocktype%", easytype)
                        .replace("%blocked%", value)
        );
    }

    public static void webhook(IEaglerPendingConnection plr, String origin, String brand, String type) {
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
        """, type, plr.getAuthUsername(), addr, protocol, origin, brand, userAgent, rewind);

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

    public static void init() {
        File motdIcon = new File(config.messages.motd.icon);
        if (!motdIcon.exists()) {
            try (InputStream in = ConfigManager.class.getResourceAsStream("/server-blocked.png")) {
                if (in != null) {
                    Files.copy(in, motdIcon.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                getLogger().warn(e.toString());
            }
        }
    }

    public static void reloadConfig() {
        config = ConfigManager.loadConfig(adapter);
    }
}
