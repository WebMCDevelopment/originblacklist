package xyz.webmc.originblacklist.base.config;

import xyz.webmc.originblacklist.base.OriginBlacklist;
import xyz.webmc.originblacklist.base.util.IOriginBlacklistPlugin;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;

public final class OriginBlacklistConfig {
  private final Json5 json5;
  private final File file;
  private final Path filePath;
  private final File iconFile;
  private final Path iconPath;
  private Json5Object config;
  private byte[] icon;
  private String icon64;

  public OriginBlacklistConfig(final IOriginBlacklistPlugin plugin) {
    this.json5 = Json5.builder(builder -> builder
        .quoteless()
        .quoteSingle()
        .parseComments()
        .writeComments()
        .prettyPrinting()
        .build());
    final String dir = "plugins/" + plugin.getPluginId();
    this.file = new File(dir + "/config.json5");
    this.filePath = file.toPath();
    this.iconFile = new File(dir + "/blacklisted.png");
    this.iconPath = iconFile.toPath();
    this.loadConfig();
  }

  private final void loadConfig() {
    try {
      this.reloadConfigUnsafe();
    } catch (final IOException exception) {
      throw new RuntimeException("Failed to load config.", exception);
    }
    this.reloadIconImage();
  }

  public final void reloadConfig() {
    try {
      this.reloadConfigUnsafe();
    } catch (final IOException exception) {
      exception.printStackTrace();
    }
    this.reloadIconImage();
  }

  private final void reloadConfigUnsafe() throws IOException {
    if (this.file.exists()) {
      String text = Files.readString(this.file.toPath(), StandardCharsets.UTF_8);
      Json5Element parsed = this.json5.parse(text);
      if (parsed instanceof Json5Object) {
        this.config = (Json5Object) parsed;
        if (merge(this.config, getDefaultConfig())) {
          this.saveConfig();
        }
      } else {
        throw new IOException("Config must be an object!");
      }
    } else {
      this.config = getDefaultConfig();
      this.saveConfig();
    }
  }

  private final void reloadIconImage() {
    try {
      if (!this.iconFile.exists()) {
        this.iconFile.getParentFile().mkdirs();
        final InputStream in = OriginBlacklist.class.getResourceAsStream("/blacklisted.png");
        Files.copy(in, iconPath, StandardCopyOption.REPLACE_EXISTING);
        in.close();
      }

      final BufferedImage img = ImageIO.read(iconFile);

      if (img.getWidth() == 64 && img.getHeight() == 64) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        this.icon64 = OriginBlacklist.getPNGBase64FromBytes(baos.toByteArray());
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
          this.icon = bytes;
        }
      } else {
        throw new IOException("Icon must be 64x64!");
      }
    } catch (final IOException exception) {
      exception.printStackTrace();
    }
  }

  public final void saveConfig() {
    try {
      this.file.getParentFile().mkdirs();
      Files.write(this.filePath, this.json5.serialize(this.config).getBytes(StandardCharsets.UTF_8));
    } catch (final IOException exception) {
      exception.printStackTrace();
    }
  }

  public final Json5Element get(final String key) {
    Json5Element element = null;

    if (this.config != null && OriginBlacklist.isNonNull(key)) {
      element = this.config;
      final String[] parts = key.split("\\.");

      for (final String part : parts) {
        if (element instanceof Json5Object) {
          final Json5Object obj = (Json5Object) element;
          if (obj.has(part)) {
            element = obj.get(part);
          } else {
            element = null;
          }
        } else {
          element = null;
        }

        if (element == null) {
          break;
        }
      }
    }

    return element;
  }

  public final String getString(final String key) {
    return this.get(key).getAsString();
  }

  public final boolean getBoolean(final String key) {
    return this.get(key).getAsBoolean();
  }

  public final int getInteger(final String key) {
    return this.get(key).getAsInt();
  }

  public final Json5Array getArray(final String key) {
    return this.get(key).getAsJson5Array();
  }

  public final Json5Object getObject(final String key) {
    return this.get(key).getAsJson5Object();
  }

  public final byte[] getIconBytes() {
    return this.icon;
  }

  public final String getIconBase64URI() {
    return this.icon64;
  }

  private static final Json5Object getDefaultConfig() {
    final Json5Object obj = new Json5Object();
    addJSONObj(obj, "debug", Json5Primitive.fromBoolean(false), null);
    final Json5Object mobj = new Json5Object();
    final Json5Array kick = new Json5Array();
    kick.add("<red>This %block_type% is %not_allowed_alt%!</red>");
    kick.add("<dark_gray>»</dark_gray> <gray>%blocked_value%</gray> <dark_gray>«</dark_gray>");
    kick.add("");
    kick.add("%action%");
    kick.add("");
    kick.add("<aqua>Think this is a mistake? Join our discord:</aqua>");
    kick.add("<blue>discord.gg/changethisintheconfig</blue>");
    addJSONObj(mobj, "kick", kick, null);
    final Json5Array motd = new Json5Array();
    motd.add("<red>This %block_type% is %not_allowed%!</red>");
    motd.add("<dark_gray>»</dark_gray> <gray>%blocked_value%</gray>");
    addJSONObj(mobj, "motd", motd, null);
    final Json5Object actions = new Json5Object();
    actions.add("generic", Json5Primitive.fromString("<gold>Please switch to a different %block_type%.</gold>"));
    actions.add("player_name", Json5Primitive.fromString("<gold>Please change your %block_type%.</gold>"));
    actions.add("ip_address", Json5Primitive.fromString("<gold>Please contact staff for assistance.</gold>"));
    addJSONObj(mobj, "actions", actions, null);
    addJSONObj(obj, "messages", mobj, null);
    final Json5Object bobj = new Json5Object();
    final Json5Array origins = new Json5Array();
    origins.add(".*eaglerhackedclients\\.vercel\\.app.*");
    origins.add(".*eaglerhacks\\.github\\.io.*");
    origins.add(".*mcproject\\.vercel\\.app.*");
    origins.add(".*wurst-b2\\.vercel\\.app.*");
    origins.add(".*flqmedev\\.github\\.io.*");
    origins.add(".*wurst2\\.vercel\\.app.*");
    origins.add(".*dhyeybg7\\.vercel\\.app.*");
    origins.add(".*uec\\.vercel\\.app.*");
    origins.add(".*valux-game\\.github\\.io.*");
    origins.add(".*project516\\.dev.*");
    addJSONObj(bobj, "origins", origins, null);
    final Json5Array brands = new Json5Array();
    brands.add(".*dragonx.*");
    brands.add(".*piclient.*");
    brands.add(".*justin.*");
    brands.add(".*wurstx.*");
    brands.add(".*moonlight.*");
    addJSONObj(bobj, "brands", brands, null);
    final Json5Array players = new Json5Array();
    players.add("Admin");
    addJSONObj(bobj, "player_names", players, null);
    final Json5Array ips = new Json5Array();
    ips.add("192.0.2.0/24");
    addJSONObj(bobj, "ip_addresses", ips, null);
    addJSONObj(obj, "blacklist", bobj, null);
    final Json5Object dobj = new Json5Object();
    addJSONObj(dobj, "enabled", Json5Primitive.fromBoolean(false), null);
    addJSONObj(dobj, "webhook_urls", new Json5Array(), null);
    addJSONObj(dobj, "send_ips", Json5Primitive.fromBoolean(true), null);
    addJSONObj(obj, "discord", dobj, null);
    final Json5Object uobj = new Json5Object();
    addJSONObj(uobj, "enabled", Json5Primitive.fromBoolean(true), null);
    addJSONObj(uobj, "allow_snapshots", Json5Primitive.fromBoolean(false), null);
    addJSONObj(uobj, "check_timer", Json5Primitive.fromNumber(3600), null);
    addJSONObj(uobj, "auto_update", Json5Primitive.fromBoolean(true), null);
    addJSONObj(obj, "update_checker", uobj, null);
    addJSONObj(obj, "blacklist_to_whitelist", Json5Primitive.fromBoolean(false), null);
    addJSONObj(obj, "block_undefined_origin", Json5Primitive.fromBoolean(false), null);
    addJSONObj(obj, "bStats", Json5Primitive.fromBoolean(true), null);
    addJSONObj(obj, "config_version", Json5Primitive.fromNumber(1), "DO NOT CHANGE");
    return obj;
  }

  private static final boolean merge(final Json5Object a, final Json5Object b) {
    boolean changed = false;

    for (String key : b.keySet()) {
      Json5Element element = b.get(key);
      if (!a.has(key)) {
        a.add(key, element.deepCopy());
        changed = true;
      } else {
        final Json5Element _element = a.get(key);
        if (_element instanceof Json5Object objA && element instanceof Json5Object objB) {
          if (merge(objA, objB)) {
            changed = true;
          }
        }
      }
    }

    return changed;
  }

  private static final void addJSONObj(final Json5Object obj, final String key, final Json5Element value,
      final String comment) {
    if (OriginBlacklist.isNonNull(comment)) {
      value.setComment(comment);
    }
    obj.add(key, value);
  }
}
