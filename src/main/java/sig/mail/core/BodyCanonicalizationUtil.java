package sig.mail.core;

import io.netty.util.internal.StringUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BodyCanonicalizationUtil {

  private static final String CRLF = "\r\n";

  public static String relaxed(String body) {
    final StringBuilder headerBuffer = new StringBuilder();
    int index = body.indexOf(CRLF);
    while (body.startsWith(CRLF, index += CRLF.length())) {
      headerBuffer.append(CRLF);
    }
    List<String> list = Arrays.stream(body.substring(index).trim().split(CRLF))
        .map(s -> {
          boolean crlf = s.endsWith(CRLF);
          s = s.replaceFirst("\\s++$", "");
          if (crlf) s += CRLF;
          return s;
        })
        .collect(Collectors.toList());
    headerBuffer.append(String.join(CRLF, list).replaceAll(" +", " ")).append(CRLF);
    return headerBuffer.toString();
  }

  //TODO: WIP, unneeded unless google changes c tag or we expand to authenticating more domains
  public static String simple(String body) {
    if (StringUtil.isNullOrEmpty(body)) return "\r\n";
    return body.trim() + "\r\n";
  }

}
