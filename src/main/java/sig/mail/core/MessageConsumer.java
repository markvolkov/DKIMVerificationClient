package sig.mail.core;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import sig.mail.core.DKIMFieldExtractor.Field;

public class MessageConsumer implements Runnable {

  @Override
  public void run() {
    String fetchRes = VerificationWaitingPool.getInstance().geNextMessageToVerify();
    if (fetchRes == null) return;
    String[] parts = fetchRes.split("RFC822.HEADER \\{.+}");
    if (parts.length != 2) return;
    try {
      String body = BodyCanonicalizationUtil.relaxed(parts[0]);
      String headers = parts[1];
      Map<String, String> dkimFields = DKIMFieldExtractor.parse(headers);
      System.out.println(DKIMFieldExtractor.getDkimValues(headers));
      //Can cache these for selector and hosts already seen
      DNSPublicRecord dnsPublicRecord = new DNSPublicRecordSupplier(dkimFields.get(Field.DOMAIN_SELECTOR.getIdentifier()), dkimFields.get(Field.HOST.getIdentifier())).get(); //Can run this on separate thread while body hash gets verified
      String bodyHash = Base64.encodeBase64String(SHA(body));
      Map<String, String> headersToHash = HeaderCanonicalizationUtil.extractHeaders(headers);
      String requiredHeaders = dkimFields.get(Field.HEADERS_TO_SIGN.getIdentifier());
      String headerHashField = HeaderCanonicalizationUtil.relaxed(requiredHeaders, headersToHash);
      String bodyHashToVerify = headerHashField + "dkim-signature:" + DKIMFieldExtractor.getDkimValues(headers).trim().replaceAll("\r\n", "").replaceAll("b=([\\S\\s]+)", "b=");
      boolean verified = bodyHash.equals(dkimFields.get(Field.CANONICALIZE_BODY.getIdentifier()));
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(dnsPublicRecord.getDecodedPublicKey());
      PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
      String hash = dkimFields.get(Field.SIGNATURE_DATA.getIdentifier());
      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initVerify(publicKey);
      sig.update(bodyHashToVerify.getBytes(StandardCharsets.UTF_8));
      verified |= sig.verify(Base64.decodeBase64(hash));
      System.out.println(verified); //if not verified then permfail
    } catch (Exception e) {
      //todo: better error handling, if theres an error verification failed(permfail)
      e.printStackTrace();
    }
  }

  private byte[] SHA(String message) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(message.getBytes(StandardCharsets.UTF_8));
    return digest.digest();
  }

}
