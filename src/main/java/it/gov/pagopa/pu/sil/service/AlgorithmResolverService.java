package it.gov.pagopa.pu.sil.service;

import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.pu.organization.dto.generated.JwtAlgorithm;
import it.gov.pagopa.pu.sil.util.CertUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class AlgorithmResolverService {
  private static final String RSA = "RSA";
  private static final String ECDSA = "EC";

  private final ConcurrentMap<Pair<JwtAlgorithm, String>, Algorithm> algorithmCache = new ConcurrentHashMap<>();

  public Algorithm resolveAlgorithm(JwtAlgorithm algorithm, String privateKey) {
    if (algorithm == null) {
      throw new IllegalArgumentException("Algorithm must not be null");
    }
    return algorithmCache.compute(Pair.of(algorithm, privateKey), (k, v) -> {
      try {
        return switch (algorithm) {
          case JwtAlgorithm.HS256 -> Algorithm.HMAC256(privateKey);
          case JwtAlgorithm.HS384 -> Algorithm.HMAC384(privateKey);
          case JwtAlgorithm.HS512 -> Algorithm.HMAC512(privateKey);
          case JwtAlgorithm.RS256 -> Algorithm.RSA256(null, CertUtils.pemKey2PrivateKey(RSA, privateKey));
          case JwtAlgorithm.RS384 -> Algorithm.RSA384(null, CertUtils.pemKey2PrivateKey(RSA, privateKey));
          case JwtAlgorithm.RS512 -> Algorithm.RSA512(null, CertUtils.pemKey2PrivateKey(RSA, privateKey));
          case JwtAlgorithm.ES256 -> Algorithm.ECDSA256(null, CertUtils.pemKey2PrivateKey(ECDSA, privateKey));
          case JwtAlgorithm.ES384 -> Algorithm.ECDSA384(null, CertUtils.pemKey2PrivateKey(ECDSA, privateKey));
          case JwtAlgorithm.ES512 -> Algorithm.ECDSA512(null, CertUtils.pemKey2PrivateKey(ECDSA, privateKey));
        };
      } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
        throw new IllegalStateException("Cannot load private key", e);
      }
    });
  }
}
