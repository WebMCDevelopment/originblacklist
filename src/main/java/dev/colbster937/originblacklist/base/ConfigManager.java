package dev.colbster937.originblacklist.base;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ConfigManager {
    public Messages messages = new Messages();
    public List<String> subscriptions;
    public Blacklist blacklist = new Blacklist();
    public Discord discord = new Discord();

    public static ConfigManager loadConfig(Base.LoggerAdapter logger) {
        File conf = new File("plugins/originblacklist/config.yml");

        try {
            if (!conf.exists()) {
                conf.getParentFile().mkdirs();
                try (InputStream in = ConfigManager.class.getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, conf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            LoaderOptions options = new LoaderOptions();
            Constructor constructor = new Constructor(ConfigManager.class, options);
            Yaml yaml = new Yaml(constructor);
            return yaml.load(new FileInputStream(conf));
        } catch (IOException e) {
            return new ConfigManager();
        }
    }

    public static class Blacklist {
        public List<String> origins;
        public List<String> brands;
        public List<String> players;
        public boolean missing_origin;
    }

    public static class Discord {
        public String webhook;
    }

    public static class Messages {
        public String kick;
        public MOTD motd;
    }

    public static class MOTD {
        public boolean enabled;
        public String text;
        public String icon;
    }
}
