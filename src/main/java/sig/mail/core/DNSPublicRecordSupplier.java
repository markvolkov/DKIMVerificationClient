package sig.mail.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;

@RequiredArgsConstructor
public class DNSPublicRecordSupplier implements Supplier<DNSPublicRecord> {

  private static final Logger LOGGER = Logger.getLogger(DNSPublicRecord.class.getName());
  private static final Pattern FIELD_PATTERN = Pattern.compile("(\\w+)=([\\S\\s][^;]+)");

  private final String domainSelector;
  private final String domain;

  @Override
  public DNSPublicRecord get() {
    String algorithm = null;
    byte[] decodedPublicKey = null;
    try {
      ProcessBuilder processBuilder = new ProcessBuilder("dig", "TXT", String.format("%s._domainkey.%s", domainSelector, domain));
      Process process = processBuilder.start();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String toParse = bufferedReader.lines().collect(Collectors.joining());
      Matcher matcher = FIELD_PATTERN.matcher(toParse);
      while(matcher.find()) {
        String group = matcher.group();
        String[] pair = group.split("=");
        String key = pair[0].trim().replace(" ", "").replace("\"", "");
        String value = pair[1].trim().replace(" ", "").replace("\"", "");
        switch (key.toLowerCase()) {
          case "v": if (!value.toUpperCase().startsWith("DKIM")) throw new IllegalArgumentException("DNS Version was incorrect or unsupported: " + value); //permfail
            break;
          case "k": algorithm = value;
            break;
          case "p": decodedPublicKey = Base64.decodeBase64(value);
            break;
        }
      }
    } catch (IOException e) {
      LOGGER.log(Level.INFO, "Error retrieving public key!", e);
      return null;
    }
    if (algorithm == null || decodedPublicKey == null) {
      return null;
    }
    return new DNSPublicRecord(algorithm, decodedPublicKey);
  }

}
