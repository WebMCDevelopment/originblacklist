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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import inet.ipaddr.IPAddress;

public class ConfigManager {
    public Messages messages = new Messages();
    //public List<String> subscriptions = List.of();
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

            Constructor constructor = new Constructor(ConfigManager.class, new LoaderOptions());
            constructor.setPropertyUtils(new org.yaml.snakeyaml.introspector.PropertyUtils() {{
                setSkipMissingProperties(true);
            }});
            Yaml y = new Yaml(constructor);
            ConfigManager l = null;

            try (InputStream in = new FileInputStream(f)) {
                l = y.load(in);
            } catch (Exception ex) {
                logger.warn("Error loading config: " + ex.getMessage());
            }

            if (l == null) {
                l = new ConfigManager();
            }

            try {
                Yaml raw = new Yaml();
                Map<String, Object> u = raw.load(new FileInputStream(f));
                Map<String, Object> d = raw.load(ConfigManager.class.getResourceAsStream("/config.yml"));
                if (mergeConfig(u, d)) saveConfig(u, f);
            } catch (Exception ex) {
                logger.warn("YAML merge error: " + ex.getMessage());
            }

            l.blacklist.resolveIPS(logger);

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

    public static void saveConfig(Map<String, Object> m, File f) throws IOException {
        DumperOptions o = new DumperOptions();
        o.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        o.setPrettyFlow(true);
        new Yaml(o).dump(m, new FileWriter(f));
    }

    public Map<String, Object> toMap() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setAllowReadOnlyProperties(true);

        Yaml yaml = new Yaml(options);
        String yaml1 = yaml.dumpAsMap(this);
        Yaml parser = new Yaml();
        Object yaml2 = parser.load(yaml1);

        return (Map<String, Object>) yaml2;
    }

    public static class Blacklist {
        public List<String> origins;
        public List<String> brands;
        public List<String> players;
        public List<String> ips = List.of();
        public transient Set<IPAddress> ips1 = new CopyOnWriteArraySet<>();
        public boolean missing_origin;
        //public String blacklist_redirect;

        public void resolveIPS(Base.LoggerAdapter logger) {
            for (String line : ips) {
                try {
                    ips1.add(new inet.ipaddr.IPAddressString(line).toAddress());
                } catch (Throwable ignored) {}
            }
        }
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
        public String ip;
    }
}
