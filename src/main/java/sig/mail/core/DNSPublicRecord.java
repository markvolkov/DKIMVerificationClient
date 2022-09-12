package sig.mail.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DNSPublicRecord {

  @Getter
  private final String algorithm;
  @Getter
  private final byte[] decodedPublicKey;

}
