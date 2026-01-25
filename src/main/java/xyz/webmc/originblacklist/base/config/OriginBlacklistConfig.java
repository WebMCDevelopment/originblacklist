package xyz.webmc.originblacklist.base.config;

import xyz.webmc.originblacklist.base.OriginBlacklist;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import de.marhali.json5.Json5Primitive;

public final class OriginBlacklistConfig {
  public static final Json5Object DEFAULT_CONFIG = getDefaultConfig();
  public static final int LATEST_CONFIG_VERSION = 2;

  private final Json5 json5;
  private final File file;
  private final Path filePath;
  private final File iconFile;
  private final Path iconPath;
  private Json5Object config;
  private byte[] icon;
  private String icon64;

  public OriginBlacklistConfig(final OriginBlacklist plugin) {
    this.json5 = Json5.builder(builder -> builder
        .quoteless()
        .quoteSingle()
        .parseComments()
        .writeComments()
        .prettyPrinting()
        .build());

    this.file = new File(plugin.getDataDir() + "/config.json5");
    this.filePath = file.toPath();
    this.iconFile = new File(plugin.getDataDir() + "/blacklisted.png");
    this.iconPath = iconFile.toPath();
    this.loadConfig();
  }

  private final void loadConfig() {
    try {
      this.reloadConfigUnsafe();
      this.reloadIconImage();
    } catch (final IOException exception) {
      throw new RuntimeException("Failed to load config.", exception);
    }
  }

  public final void reloadConfig() {
    try {
      this.reloadConfigUnsafe();
      this.reloadIconImage();
    } catch (final IOException exception) {
      exception.printStackTrace();
    }
  }

  private final void reloadConfigUnsafe() throws IOException {
    if (this.file.exists()) {
      String text = Files.readString(this.file.toPath(), StandardCharsets.UTF_8);
      Json5Element parsed = this.json5.parse(text);
      if (parsed instanceof Json5Object) {
        this.config = (Json5Object) parsed;
        this.config = OriginBlacklistConfigTransformer.transformConfig(this.config);
        merge(this.config, DEFAULT_CONFIG);
      } else {
        throw new IOException("Config must be an object!");
      }
    } else {
      this.config = DEFAULT_CONFIG;
    }
    this.saveConfig();
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
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        this.icon64 = OriginBlacklist.getPNGBase64FromBytes(baos.toByteArray());
        final byte[] bytes = new byte[64 * 64 * 4];
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
      final String[] parts = splitPath(key);

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

  public final boolean set(final String key, final Json5Element value) {
    boolean ret = false;

    if (this.config != null && value != null) {
      final String[] parts = splitPath(key);

      if (parts.length > 0) {
        Json5Object obj = this.config;

        for (int i = 0; i < parts.length - 1; i++) {
          final String part = parts[i];
          final Json5Element cur = obj.has(part) ? obj.get(part) : null;

          if (cur instanceof Json5Object next) {
            obj = next;
          } else {
            final Json5Object next = new Json5Object();
            obj.add(part, next);
            obj = next;
          }
        }

        obj.add(parts[parts.length - 1], value.deepCopy());
        this.saveConfig();
        ret = true;
      }
    }

    return ret;
  }

  public final boolean remove(final String key) {
    boolean ret = false;

    if (this.config != null) {
      final String[] parts = splitPath(key);

      if (parts.length > 0) {
        Json5Object obj = this.config;
        Json5Element element = obj;

        for (int i = 0; i < parts.length - 1; i++) {
          if (element instanceof Json5Object cur && cur.has(parts[i])) {
            element = cur.get(parts[i]);
            if (element instanceof Json5Object) {
              obj = (Json5Object) element;
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

        if (element != null && obj.has(parts[parts.length - 1])) {
          obj.remove(parts[parts.length - 1]);
          this.saveConfig();
          ret = true;
        }
      }
    }

    return ret;
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
    final Json5Object mObj = new Json5Object();
    final Json5Array kick = new Json5Array();
    kick.add("<red>This %block_type% is %not_allowed_alt%!</red>");
    kick.add("<dark_gray>»</dark_gray> <gray>%blocked_value%</gray> <dark_gray>«</dark_gray>");
    kick.add("");
    kick.add("%action%");
    kick.add("");
    kick.add("<aqua>Think this is a mistake? Join our discord:</aqua>");
    kick.add("<blue>%discord_invite%</blue>");
    addJSONObj(mObj, "kick", kick, null);
    final Json5Object actions = new Json5Object();
    actions.add("generic", Json5Primitive.fromString("<gold>Please switch to a different %block_type%.</gold>"));
    actions.add("player_name", Json5Primitive.fromString("<gold>Please change your %block_type%.</gold>"));
    actions.add("ip_address", Json5Primitive.fromString("<gold>Please contact staff for assistance.</gold>"));
    addJSONObj(mObj, "actions", actions, null);
    addJSONObj(obj, "messages", mObj, null);
    final Json5Object nObj = new Json5Object();
    addJSONObj(nObj, "enabled", Json5Primitive.fromBoolean(true), null);
    final Json5Array mArr = new Json5Array();
    mArr.add("<red>This %block_type% is %not_allowed%!</red>");
    mArr.add("<dark_gray>»</dark_gray> <gray>%blocked_value%</gray>");
    addJSONObj(nObj, "text", mArr, null);
    final Json5Object mPlayers = new Json5Object();
    addJSONObj(mPlayers, "online", Json5Primitive.fromNumber(0), null);
    addJSONObj(mPlayers, "max", Json5Primitive.fromNumber(0), null);
    final Json5Array hArr = new Json5Array();
    hArr.add("<blue>Join our discord</blue>");
    hArr.add("<blue>%discord_invite%</blue>");
    addJSONObj(mPlayers, "hover", hArr, null);
    addJSONObj(nObj, "players", mPlayers, null);
    addJSONObj(obj, "motd", nObj, null);
    final Json5Object bObj = new Json5Object();
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
    addJSONObj(bObj, "origins", origins, null);
    final Json5Array brands = new Json5Array();
    brands.add(".*dragonx.*");
    brands.add(".*piclient.*");
    brands.add(".*justin.*");
    brands.add(".*wurstx.*");
    brands.add(".*moonlight.*");
    addJSONObj(bObj, "brands", brands, null);
    final Json5Array players = new Json5Array();
    players.add("Admin");
    addJSONObj(bObj, "player_names", players, null);
    final Json5Array ips = new Json5Array();
    ips.add("192.0.2.0/24");
    addJSONObj(bObj, "ip_addresses", ips, null);
    addJSONObj(obj, "blacklist", bObj, null);
    final Json5Object dObj = new Json5Object();
    addJSONObj(dObj, "invite", Json5Primitive.fromString("discord.gg/changeme"), null);
    final Json5Object webhook = new Json5Object();
    addJSONObj(webhook, "enabled", Json5Primitive.fromBoolean(false), null);
    addJSONObj(webhook, "webhook_urls", new Json5Array(), null);
    addJSONObj(webhook, "send_ips", Json5Primitive.fromBoolean(true), null);
    addJSONObj(dObj, "webhook", webhook, null);
    addJSONObj(obj, "discord", dObj, null);
    final Json5Object uObj = new Json5Object();
    addJSONObj(uObj, "enabled", Json5Primitive.fromBoolean(true), null);
    addJSONObj(uObj, "allow_snapshots", Json5Primitive.fromBoolean(false), null);
    addJSONObj(uObj, "check_timer", Json5Primitive.fromNumber(3600), null);
    addJSONObj(uObj, "auto_update", Json5Primitive.fromBoolean(true), null);
    addJSONObj(obj, "update_checker", uObj, null);
    addJSONObj(obj, "blacklist_http_api", Json5Primitive.fromBoolean(false), null);
    addJSONObj(obj, "blacklist_to_whitelist", Json5Primitive.fromBoolean(false), null);
    addJSONObj(obj, "block_undefined_origin", Json5Primitive.fromBoolean(false), null);
    addJSONObj(obj, "bStats", Json5Primitive.fromBoolean(true), null);
    addJSONObj(obj, "logFile", Json5Primitive.fromBoolean(true), null);
    addJSONObj(obj, "config_version", Json5Primitive.fromNumber(LATEST_CONFIG_VERSION), "DO NOT CHANGE");
    return obj;
  }

  private static final boolean merge(final Json5Object aObj, final Json5Object bObj) {
    final Json5Object ret = new Json5Object();
    boolean changed = false;
    for (final String key : bObj.keySet()) {
      final Json5Element dv = bObj.get(key);
      if (aObj.has(key)) {
        final Json5Element v = aObj.get(key);
        if (dv instanceof Json5Object) {
          if (v instanceof Json5Object) {
            final boolean c = merge((Json5Object) v, (Json5Object) dv);
            ret.add(key, (Json5Object) v);
            if (c) {
              changed = true;
            }
          } else {
            ret.add(key, dv.deepCopy());
            changed = true;
          }
        } else if (dv instanceof Json5Array) {
          if (v instanceof Json5Array) {
            final Json5Array vArr = (Json5Array) v;
            final Json5Array dArr = (Json5Array) dv;
            if (dArr.size() == 0) {
              ret.add(key, vArr.deepCopy());
            } else {
              final Json5Element d0 = dArr.get(0);
              if (d0 instanceof Json5Primitive && ((Json5Primitive) d0).isString()) {
                final Json5Array out = new Json5Array();
                for (final Json5Element e : vArr) {
                  if (e instanceof Json5Primitive && ((Json5Primitive) e).isString()) {
                    out.add(e.deepCopy());
                  }
                }
                if (out.size() > 0) {
                  if (out.size() != vArr.size()) {
                    changed = true;
                  }
                  ret.add(key, out);
                } else {
                  ret.add(key, dArr.deepCopy());
                  changed = true;
                }
              } else {
                boolean a = true;
                for (final Json5Element e : vArr) {
                  if (e == null) {
                    a = false;
                  } else if (d0 instanceof Json5Object) {
                    if (!(e instanceof Json5Object)) {
                      a = false;
                    }
                  } else if (d0 instanceof Json5Array) {
                    if (!(e instanceof Json5Array)) {
                      a = false;
                    }
                  } else if (d0 instanceof Json5Primitive && e instanceof Json5Primitive) {
                    final Json5Primitive bp = (Json5Primitive) d0;
                    final Json5Primitive ap = (Json5Primitive) e;
                    if (bp.isBoolean()) {
                      if (!ap.isBoolean()) {
                        a = false;
                      }
                    } else if (bp.isNumber()) {
                      if (!ap.isNumber()) {
                        a = false;
                      }
                    } else if (bp.isString()) {
                      if (!ap.isString()) {
                        a = false;
                      }
                    }
                  } else {
                    a = false;
                  }
                  if (!a) {
                    break;
                  }
                }
                if (a) {
                  ret.add(key, vArr.deepCopy());
                } else {
                  ret.add(key, dArr.deepCopy());
                  changed = true;
                }
              }
            }
          } else {
            ret.add(key, dv.deepCopy());
            changed = true;
          }
        } else if (dv instanceof Json5Primitive) {
          if (v instanceof Json5Primitive) {
            final Json5Primitive dp = (Json5Primitive) dv;
            final Json5Primitive vp = (Json5Primitive) v;
            if (dp.isBoolean()) {
              if (vp.isBoolean()) {
                ret.add(key, vp.deepCopy());
              } else {
                ret.add(key, dv.deepCopy());
                changed = true;
              }
            } else if (dp.isNumber()) {
              if (vp.isNumber()) {
                ret.add(key, vp.deepCopy());
              } else {
                ret.add(key, dv.deepCopy());
                changed = true;
              }
            } else if (dp.isString()) {
              if (vp.isString()) {
                ret.add(key, vp.deepCopy());
              } else {
                ret.add(key, dv.deepCopy());
                changed = true;
              }
            } else {
              ret.add(key, dv.deepCopy());
              changed = true;
            }
          } else {
            ret.add(key, dv.deepCopy());
            changed = true;
          }
        } else {
          ret.add(key, dv.deepCopy());
          changed = true;
        }
      } else {
        ret.add(key, dv.deepCopy());
        changed = true;
      }
    }
    for (final String key : aObj.keySet()) {
      if (!bObj.has(key)) {
        ret.add(key, aObj.get(key).deepCopy());
      }
    }
    for (final String key : aObj.keySet()) {
      if (!bObj.has(key)) {
        ret.add(key, aObj.get(key).deepCopy());
      }
    }
    for (final String k : new ArrayList<>(aObj.keySet())) {
      aObj.remove(k);
    }
    for (final String k : ret.keySet()) {
      aObj.add(k, ret.get(k));
    }
    for (final String key : ret.keySet()) {
      aObj.add(key, ret.get(key));
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

  private static final String[] splitPath(final String key) {
    final String[] ret;

    if (OriginBlacklist.isNonNull(key)) {
      ret = key.split("\\.");
    } else {
      ret = new String[0];
    }

    return ret;
  }
}
