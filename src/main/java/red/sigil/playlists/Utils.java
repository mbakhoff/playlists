package red.sigil.playlists;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

  public static String escapeHtml(String string) {
    if (string == null)
      return null;

    StringBuilder escapedTxt = new StringBuilder();
    for (int i = 0; i < string.length(); i++) {
      char tmp = string.charAt(i);
      switch (tmp) {
        case '<':
          escapedTxt.append("&lt;");
          break;
        case '>':
          escapedTxt.append("&gt;");
          break;
        case '&':
          escapedTxt.append("&amp;");
          break;
        case '"':
          escapedTxt.append("&quot;");
          break;
        case '\'':
          escapedTxt.append("&#x27;");
          break;
        case '/':
          escapedTxt.append("&#x2F;");
          break;
        default:
          escapedTxt.append(tmp);
      }
    }
    return escapedTxt.toString();
  }

  public static String parseListId(String url) {
    // e.g. https://www.youtube.com/playlist?list=PL7USMo--IcSi5p44jTZTezqS3Z-MEC6DU
    Matcher matcher = Pattern.compile(".*[?&]list=([A-Za-z0-9\\-_]+).*").matcher(url);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException(url);
  }
}
