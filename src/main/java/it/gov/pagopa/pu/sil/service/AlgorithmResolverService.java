package it.gov.pagopa.pu.sil.service;

import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.pu.organization.dto.generated.JwtAlgorithm;
import it.gov.pagopa.pu.sil.exception.SigningKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
@Component
public class AlgorithmResolverService {
  private static final String RSA = "RSA";
  private static final String ECDSA = "ECDSA";

  public Algorithm resolveAlgorithm(JwtAlgorithm algorithm, byte[] signingKey) {
    if (algorithm == null) {
      throw new IllegalArgumentException("Algorithm must not be null");
    }
    return switch (algorithm) {
      case JwtAlgorithm.HS256 -> Algorithm.HMAC256(signingKey);
      case JwtAlgorithm.HS384 -> Algorithm.HMAC384(signingKey);
      case JwtAlgorithm.HS512 -> Algorithm.HMAC512(signingKey);
      case JwtAlgorithm.RS256 -> Algorithm.RSA256(null, signingKey2PrivateKey(RSA, signingKey));
      case JwtAlgorithm.RS384 -> Algorithm.RSA384(null, signingKey2PrivateKey(RSA, signingKey));
      case JwtAlgorithm.RS512 -> Algorithm.RSA512(null, signingKey2PrivateKey(RSA, signingKey));
      case JwtAlgorithm.ES256 -> Algorithm.ECDSA256(null, signingKey2PrivateKey(ECDSA, signingKey));
      case JwtAlgorithm.ES384 -> Algorithm.ECDSA384(null, signingKey2PrivateKey(ECDSA, signingKey));
      case JwtAlgorithm.ES512 -> Algorithm.ECDSA512(null, signingKey2PrivateKey(ECDSA, signingKey));
    };
  }

  private static <T> T signingKey2PrivateKey(String algorithm, byte[] signingKey) {
    try {
      X509EncodedKeySpec privateKeyX509 = new X509EncodedKeySpec(signingKey);
      KeyFactory kf = KeyFactory.getInstance(algorithm);
      return (T) kf.generatePrivate(privateKeyX509);
    } catch (Exception e) {
      throw new SigningKeyException("Unable to generate private key for algorithm: %s".formatted(algorithm));
    }
  }
}
