package xyz.webmc.originblacklist.base.util;

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

  public static final boolean checkForUpdate(final String repo, final Semver currentVersion, final boolean allowPreRelease) {
    try {
      final URL url = new URL("https://api.github.com/repos/" + repo + "/releases");
      final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      conn.connect();
      final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      Json5Element element = json5.parse(reader);
      if (element instanceof Json5Array) {
        final Json5Array arr = element.getAsJson5Array();
        if (arr.size() > 0) {
          element = arr.get(0);
          if (element instanceof Json5Object) {
            final Json5Object obj = element.getAsJson5Object();
            final String tag = obj.get("tag_name").getAsString();
            final Semver ver = new Semver(tag.substring(1));
            if (ver.isGreaterThan(currentVersion) && (allowPreRelease || currentVersion.diff(ver) != VersionDiff.BUILD)) {
              return true;
            }
          }
        }
      }
      conn.disconnect();
      return false;
    } catch (final Throwable t) {
      t.printStackTrace();
      return false;
    }
  }
}
