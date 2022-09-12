package sig.mail.core;

import io.netty.util.internal.StringUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class DKIMFieldExtractor {

  private static final Logger LOGGER = Logger.getLogger(DKIMFieldExtractor.class.getName());
  private static final String DKIM_HEADER = "DKIM-Signature: ";
  private static final Pattern DKIM_PATTERN = Pattern.compile(DKIM_HEADER + "([\\S\\s]+?(={1,2}[^\\S]+))");

  @RequiredArgsConstructor
  public enum Field {
    VERSION("v"),
    ALGORITHM("a"),
    SIGNATURE_DATA("b"),
    CANONICALIZE_BODY("bh"),
    CANONICALIZE_TYPE("c"),
    HOST("d")/*SDID*/,
    HEADERS_TO_SIGN("h"),
    DOMAIN_SELECTOR("s");
    @Getter private final String identifier;
  }

  public static String getDkimValues(final String headers) {
    Matcher matcher = DKIM_PATTERN.matcher(headers);
    if (!matcher.find()) throw new IllegalArgumentException("Cannot find DKIM-Signature in headers");
    String matched = matcher.group();
    if (StringUtil.isNullOrEmpty(matched)) throw new IllegalArgumentException("DKIM-Signature was null or empty");
    return matched.substring(DKIM_HEADER.length());
  }

  public static String getDkim(final String headers) {
    Matcher matcher = DKIM_PATTERN.matcher(headers);
    if (!matcher.find()) throw new IllegalArgumentException("Cannot find DKIM-Signature in headers");
    String matched = matcher.group();
    if (StringUtil.isNullOrEmpty(matched)) throw new IllegalArgumentException("DKIM-Signature was null or empty");
    return matched;
  }

  public static Map<String, String> parse(final String headers) {
    Matcher matcher = DKIM_PATTERN.matcher(headers);
    if (!matcher.find()) throw new IllegalArgumentException("Cannot find DKIM-Signature in headers");
    String matched = matcher.group();
    if (StringUtil.isNullOrEmpty(matched)) throw new IllegalArgumentException("DKIM-Signature was null or empty");
    final Map<String, String> fieldMap = new HashMap<>();
    String fieldsToParse = matched.substring(DKIM_HEADER.length());
    String[] fields = fieldsToParse.split(";");
    for (String field : fields) {
      String[] pair = field.split("=", 2);
      String key = pair[0].trim();
      String value = pair[1].trim();
      fieldMap.put(key, value);
    }
    return fieldMap;
  }

}
