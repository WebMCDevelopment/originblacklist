package dev.colbster937.originblacklist.base;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPlayer;
import net.lax1dude.eaglercraft.backend.server.api.EnumWebSocketHeader;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftInitializePlayerEvent;
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

    public static void handleMOTD(IEaglercraftMOTDEvent e) {
        if (config.messages.motd.enabled) {
            IMOTDConnection conn = e.getMOTDConnection();
            String origin = conn.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
            List<String> m = List.of(config.messages.motd.text.split("\n")).stream()
                    .map(line -> line
                            .replace("%blocktype%", "origin")
                            .replace("%easyblocktype%", "website")
                            .replace("%blocked%", origin))
                    .map(line -> LegacyComponentSerializer.legacySection()
                            .serialize(MiniMessage.miniMessage().deserialize(line)))
                    .collect(Collectors.toList());
            if ((origin != "null" || origin != null) && !config.blacklist.missing_origin) {
                for (String origin1 : config.blacklist.origins) {
                    if (matches(origin, origin1)) {
                        setMOTD(conn, m);
                        return;
                    }
                }
            } else {
                setMOTD(conn, m);
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
