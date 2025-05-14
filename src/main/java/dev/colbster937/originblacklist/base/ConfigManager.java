package dev.colbster937.originblacklist.base;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.DumperOptions;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    public Messages messages = new Messages();
    public List<String> subscriptions = List.of();
    public Blacklist blacklist = new Blacklist();
    public Discord discord = new Discord();

    public static ConfigManager loadConfig(Base.LoggerAdapter logger) {
        File f = new File("plugins/originblacklist/config.yml");

        try {
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                try (InputStream in = ConfigManager.class.getResourceAsStream("/config.yml")) {
                    if (in != null) Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Yaml y = new Yaml(new Constructor(ConfigManager.class, new LoaderOptions()));
            ConfigManager l;
            try (InputStream in = new FileInputStream(f)) { l = y.load(in); }

            if (l == null) l = new ConfigManager();

            Yaml raw = new Yaml();
            Map<String, Object> u = raw.load(new FileInputStream(f));
            Map<String, Object> d = raw.load(ConfigManager.class.getResourceAsStream("/config.yml"));
            if (mergeConfig(u, d)) saveConfig(u, f);

            return l;
        } catch (IOException e) {
            return new ConfigManager();
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean mergeConfig(Map<String, Object> u, Map<String, Object> d) {
        boolean c = false;
        for (String k : d.keySet()) {
            if (!u.containsKey(k)) {
                u.put(k, d.get(k));
                c = true;
            } else if (u.get(k) instanceof Map && d.get(k) instanceof Map)
                c |= mergeConfig((Map<String, Object>) u.get(k), (Map<String, Object>) d.get(k));
        }
        return c;
    }

    private static void saveConfig(Map<String, Object> m, File f) throws IOException {
        DumperOptions o = new DumperOptions();
        o.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        o.setPrettyFlow(true);
        new Yaml(o).dump(m, new FileWriter(f));
    }

    public static class Blacklist {
        public List<String> origins;
        public List<String> brands;
        public List<String> players;
        public boolean missing_origin;
        public String blacklist_redirect;
    }

    public static class Discord {
        public String webhook;
    }

    public static class Messages {
        public String kick;
        public MOTD motd;
        public Help help;
    }

    public static class MOTD {
        public boolean enabled;
        public String text;
        public String icon;
    }

    public static class Help {
        public String generic;
        public String player;
    }
}
