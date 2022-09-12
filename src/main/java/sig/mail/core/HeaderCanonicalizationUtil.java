package sig.mail.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class HeaderCanonicalizationUtil {

  private static final HeaderField[] HEADER_FIELDS_VALUES = HeaderField.values();
  private static final Pattern REQUIRED_HEADERS_PATTERN;

  static {
    StringBuilder requiredHeaderPatternString = new StringBuilder();
    for (int i = 0; i < HEADER_FIELDS_VALUES.length; i++) {
      requiredHeaderPatternString.append(HEADER_FIELDS_VALUES[i].getIdentifier());
      if (i != HEADER_FIELDS_VALUES.length - 1) requiredHeaderPatternString.append("|");
    }
    REQUIRED_HEADERS_PATTERN = Pattern.compile("(" + requiredHeaderPatternString.toString() + "):\\s.+", Pattern.CASE_INSENSITIVE);
  }

  @RequiredArgsConstructor
  public enum HeaderField {
    TO("to"),
    CC("cc"),
    SUBJECT("subject"),
    MSG_ID("message-id"),
    DATE("date"),
    FROM("from"),
    MIME_VERSION("mime-version"),
    FEEDBACK_ID("feedback-id"),
    CONTENT_TYPE("content-type"),
    REFERENCES("references"),
    REPLY_TO("reply-to"),
    X_MS_Exchange_SENDERADCHECK("x-ms-exchange-senderadcheck");
    @Getter private final String identifier;
  }

  public static Map<String, String> extractHeaders(final String headerString) {
    final Map<String, String> headers = new HashMap<>();
    Matcher matcher = REQUIRED_HEADERS_PATTERN.matcher(headerString);
    while(matcher.find()) {
      String group = matcher.group();
      String[] splitGroup = group.split(":", 2);
      String headerKey = splitGroup[0].trim();
      String headerValue = splitGroup[1].trim();
      headers.put(headerKey.toLowerCase(), headerValue);
    }
    return headers;
  }

  public static String relaxed(String requiredHashFields, Map<String, String> headers) {
    List<String> duplicatesRemovedParts = Arrays.stream(requiredHashFields.trim().split(":"))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(headers::containsKey)
            .distinct()
            .collect(Collectors.toList());
    final StringBuilder result = new StringBuilder();
    for (String key : duplicatesRemovedParts) {
      String value = headers.get(key);
      value = value.replaceAll(" +", " ");
      result.append(key).append(":").append(value).append("\r\n");
    }
    return result.toString();
  }

}

