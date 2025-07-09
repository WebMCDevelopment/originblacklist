package dev.colbster937.originblacklist.base;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.*;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftMOTDEvent;
import net.lax1dude.eaglercraft.backend.server.api.query.IMOTDConnection;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Base {
    private static LoggerAdapter adapter;
    private static IEaglerXServerAPI api;
    private static IPBlacklist ipblacklist;

    public static void setLogger(LoggerAdapter log) {
        adapter = log;
    }

    public static void setApi(IEaglerXServerAPI api1) {
        api = api1;
    }

    public static ConfigManager config;

    public static String pluginVer = "1.0.2";

    public static boolean checkVer(String v1, String v2) {
        String[] c = v1.split("\\.");
        String[] r = v2.split("\\.");
        for (int i = 0; i < Math.max(c.length, r.length); i++) {
            int c1 = i < c.length ? Integer.parseInt(c[i]) : 0;
            int r1 = i < r.length ? Integer.parseInt(r[i]) : 0;
            if (c1 < r1)
                return false;
            if (c1 > r1)
                return true;
        }
        return true;
    }

    public static LoggerAdapter getLogger() {
        if (adapter == null)
            throw new IllegalStateException("Logger not initialized!");
        return adapter;
    }

    public interface LoggerAdapter {
        void info(String msg);

        void warn(String msg);

        void error(String msg);
    }

    public static void handleConnection(IEaglercraftLoginEvent e) {
        IEaglerLoginConnection conn = e.getLoginConnection();
        String origin = conn.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
        String brand = conn.getEaglerBrandString();
        String name = conn.getUsername();
        String notAllowed1 = "not allowed on the server";
        String notAllowed2 = "not allowed";

        if (origin != null && !origin.equals("null")) {
            for (String origin1 : config.blacklist.origins) {
                if (matches(origin, origin1)) {
                    setKick(e, formatKickMessage("origin", "website", notAllowed1, notAllowed2, origin, conn.getWebSocketHost()));
                    webhook(conn, origin, brand, "origin");
                    return;
                }
            }
        }

        if (brand != null && !brand.equals("null")) {
            for (String brand1 : config.blacklist.brands) {
                if (matches(brand, brand1)) {
                    setKick(e, formatKickMessage("brand", "client", notAllowed1, notAllowed2, brand, conn.getWebSocketHost()));
                    webhook(conn, origin, brand, "brand");
                    return;
                }
            }
        }

        if (name != null && !name.equals("null")) {
            for (String name1 : config.blacklist.players) {
                if (matches(name, name1) || (name.length() > 16 || name.length() < 3)) {
                    setKick(e, formatKickMessage("player", "username", notAllowed1, notAllowed2, name, conn.getWebSocketHost()));
                    webhook(conn, origin, name, "player");
                    return;
                }
            }
        }
    }

    public static void setKick(IEaglercraftLoginEvent e, Component msg) {
        try {
            getLogger().info("Kicked " + e.getProfileUsername());
            e.setKickMessage(msg);
        } catch (Throwable ignored) {
            String msg1 = LegacyComponentSerializer.legacySection().serialize(msg);
            e.setKickMessage(msg1);
        }
    }

    public static void handleMOTD(IEaglercraftMOTDEvent e) {
        if (config.messages.motd.enabled) {
            IMOTDConnection conn = e.getMOTDConnection();
            String origin = conn.getWebSocketHeader(EnumWebSocketHeader.HEADER_ORIGIN);
            List<String> m = List.of(config.messages.motd.text.split("\n")).stream()
                    .map(line -> line
                            .replaceAll("%blocktype%", "origin")
                            .replaceAll("%easyblocktype%", "website")
                            .replaceAll("%notallowed1%", "blacklisted")
                            .replaceAll("%notallowed2%", "blacklisted")
                            .replaceAll("%blocked%", origin)
                            .replaceAll("%host%", conn.getWebSocketHost()))
                    .map(line -> LegacyComponentSerializer.legacySection().serialize(
                            MiniMessage.miniMessage().deserialize(line)))
                    .collect(Collectors.toList());

            if (origin != null && !origin.equals("null")) {
                for (String origin1 : config.blacklist.origins) {
                    if (matches(origin, origin1)) {
                        setMOTD(conn, m);
                        return;
                    }
                }
            }
        }
    }

    public static void setMOTD(IMOTDConnection conn, List<String> m) {
        conn.setServerMOTD(m);
        conn.setPlayerTotal(0);
        conn.setPlayerMax(0);
        conn.setPlayerList(List.of());

        if (config.messages.motd.icon != null && !config.messages.motd.icon.isEmpty()) {
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
    }

    public static String handlePre(String ip, String name) {
        if (ip != null && !ip.equalsIgnoreCase("null")) {
            for (String ip1 : Base.config.blacklist.ips) {
                if (ipblacklist.check(ip)) {
                    Component kick = formatKickMessage("ip address", "ip", "blacklisted", "blacklisted", ip, "");
                    return LegacyComponentSerializer.legacySection().serialize(kick);
                }
            }
        }
        return "false";
    }

    public static boolean matches(String text1, String text2) {
        return text1.toLowerCase().matches(text2.replace(".", "\\.").replaceAll("\\*", ".*").toLowerCase());
    }

    public static Component formatKickMessage(String type, String easytype, String notAllowed1, String notAllowed2, String value, String host) {
        String help = "";
        if (type != "player") {
            help = config.messages.help.generic;
        } else if (type == "ip") {
            help = config.messages.help.ip;
        } else {
            help = config.messages.help.player;
        }
        return MiniMessage.miniMessage().deserialize(
                config.messages.kick
                        .replaceAll("%help%", help)
                        .replaceAll("%blocktype%", type)
                        .replaceAll("%easyblocktype%", easytype)
                        .replaceAll("%notallowed1%", notAllowed1)
                        .replaceAll("%notallowed2%", notAllowed2)
                        .replaceAll("%blocked%", value)
                        .replaceAll("%host%", host));
    }

    public static void webhook(IEaglerLoginConnection plr, String origin, String brand, String type) {
        String webhook = config.discord.webhook;
        if (webhook == null || webhook.isBlank())
            return;

        CompletableFuture.runAsync(() -> {
            String addr = getAddr(plr);
            int protocol = !plr.isEaglerXRewindPlayer() ? plr.getMinecraftProtocol() : plr.getRewindProtocolVersion();
            String host = plr.getWebSocketHost();
            String userAgent = plr.getWebSocketHeader(EnumWebSocketHeader.HEADER_USER_AGENT);
            Boolean rewind = plr.isEaglerXRewindPlayer();
            if (userAgent == null || userAgent.isEmpty())
                userAgent = "undefined";

            String payload = String.format(
                    """
                            {
                              "content": "Blocked a blacklisted %s from joining",
                              "embeds": [
                                {
                                  "title": "Player Information",
                                  "description": "🎮 **Name:** %s\\n🏠 **IP:** %s\\n🌄 **PVN:** %s\\n🌐 **Origin:** %s\\n🔋 **Brand:** %s\\n🪑 **Host:** %s\\n🧊 **User Agent:** %s\\n⏪ **Rewind:** %s"
                                }
                              ]
                            }
                            """,
                    type, plr.getUsername(), addr, protocol, origin, brand, plr.isWebSocketSecure() ? "wss://" : "ws://" + host, userAgent, rewind ? "Yes" : "No");

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
        });
    }

    public static String getAddr(IEaglerLoginConnection plr) {
        var addr1 = plr.getPlayerAddress() != null ? plr.getPlayerAddress().toString().substring(1) : "undefined:undefined";
        var addr2 = addr1.lastIndexOf(':') != -1 ? addr1.substring(0, addr1.lastIndexOf(':')) : addr1;
        return addr2;
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
        ipblacklist = new IPBlacklist();
    }

    public static void reloadConfig() {
        config = ConfigManager.loadConfig(adapter);
    }
}
