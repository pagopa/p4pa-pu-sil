package it.gov.pagopa.pu.sil.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CertUtils {
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private CertUtils(){}

  public static <T> T pemPub2PublicKey(String algorithm, String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    String pubStringFormat = extractInlinePemBody(publicKey);
    try(
      InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pubStringFormat))
    ) {
      X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
      KeyFactory kf = KeyFactory.getInstance(algorithm);
      return (T) kf.generatePublic(encodedKeySpec);
    }
  }

  public static <T> T pemKey2PrivateKey(String algorithm, String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
    String keyStringFormat =  extractInlinePemBody(privateKey);
    try(
      InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(keyStringFormat))
    ) {
      PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
      KeyFactory kf = KeyFactory.getInstance(algorithm);
      return (T) kf.generatePrivate(encodedKeySpec);
    }
  }

  public static String extractInlinePemBody(String target) {
    return target
      .replaceAll("^-----BEGIN[A-Z|\\s]+-----", "")
      .replaceAll("\\s+", "")
      .replaceAll("-----END[A-Z|\\s]+-----$", "");
  }
}
