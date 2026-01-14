package xyz.webmc.originblacklist.base.util;

import xyz.webmc.originblacklist.base.OriginBlacklist;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Object;
import org.semver4j.Semver;
import org.semver4j.Semver.VersionDiff;

public class UpdateChecker {
  private static final Json5 json5 = Json5.builder(builder -> builder.build());

  public static final String checkForUpdates(final String repo, final Semver currentVersion, final boolean allowSnapshots) {
    try {
      final URL url = new URL("https://api.github.com/repos/" + repo + "/releases");
      final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      conn.setRequestProperty("User-Agent", OriginBlacklist.getUserAgent());
      conn.setRequestProperty("Accept", "application/vnd.github+json");
      conn.connect();
      final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String ret = null;
      Json5Element element = json5.parse(reader);
      reader.close();

      Json5Object rel = null;
      Json5Object snap = null;

      if (element instanceof Json5Array) {
        final Json5Array arr = element.getAsJson5Array();
        for (int i = 0; i < arr.size(); i++) {
          element = arr.get(i);
          if (element instanceof Json5Object) {
            final Json5Object obj = element.getAsJson5Object();
            if (!obj.has("published_at")) {
              continue;
            }
            final boolean pre = obj.get("prerelease").getAsBoolean();
            if (!pre) {
              if (rel == null || obj.get("published_at").getAsString().compareTo(rel.get("published_at").getAsString()) > 0) {
                rel = obj;
              }
            } else {
              if (snap == null || obj.get("published_at").getAsString().compareTo(snap.get("published_at").getAsString()) > 0) {
                snap = obj;
              }
            }
          }
          continue;
        }

        for (int i = 0; i < 2; i++) {
          final Json5Object obj = i == 0 ? rel : snap;
          if (obj == null || (i == 1 && !allowSnapshots)) {
            continue;
          }
          final String tag = obj.get("tag_name").getAsString();
          final Semver ver = new Semver(tag.startsWith("v") ? tag.substring(1) : tag);
          String comm;
          try {
            comm = ver.getBuild().get(0).trim();
          } catch (Throwable t) {
            comm = "";
          }
          if (ver.isGreaterThan(currentVersion) || (allowSnapshots && currentVersion.diff(ver) == VersionDiff.BUILD && OriginBlacklist.isNonNull(comm) && !BuildInfo.get("git_cm_hash").startsWith(comm))) {
            element = obj.get("assets");
            if (element instanceof Json5Array) {
              final Json5Array aArr = element.getAsJson5Array();
              if (aArr.size() > 0) {
                element = aArr.get(0);
                if (element instanceof Json5Object) {
                  ret = element.getAsJson5Object().get("browser_download_url").getAsString();
                  break;
                }
              }
            }
          }
        }
      }

      conn.disconnect();
      return ret;
    } catch (final Throwable t) {
      t.printStackTrace();
      return null;
    }
  }
}
