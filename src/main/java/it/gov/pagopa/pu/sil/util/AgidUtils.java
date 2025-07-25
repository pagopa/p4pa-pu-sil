package it.gov.pagopa.pu.sil.util;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.gov.pagopa.pu.sil.config.agid.PuIntegrityDataConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AgidUtils {
  private AgidUtils() {
  }

  private static final String CONTENT_ENCODING_LOWERCASE = HttpHeaders.CONTENT_ENCODING.toLowerCase();
  private static final String CONTENT_TYPE_LOWERCASE = HttpHeaders.CONTENT_TYPE.toLowerCase();

  private static final Map<String, String> publicKeyToKidCache = new HashMap<>();

  public static SignedJWT signJwtRSA(JWTClaimsSet claims, String kid, RSASSASigner jwsRsaSigner) {
    return JWTUtils.signJwt(claims, kid, JWSAlgorithm.RS256, jwsRsaSigner);
  }

  public static String buildDigest(String value) {
    return "SHA-256=" + CryptoUtils.sha256Base64(value);
  }

  public static String buildAgidJwtSignature(String digest, PuIntegrityDataConfig puIntegrityDataConfig, RSASSASigner rsaJwsSigner) {
    String kid = publicKeyToKidCache.computeIfAbsent(puIntegrityDataConfig.getPublicKey(),
      publicKey -> UUID.nameUUIDFromBytes(puIntegrityDataConfig.getPublicKey().getBytes(StandardCharsets.UTF_8)).toString());

    return signJwtRSA(
      buildAgidJwtSignatureClaims(digest, puIntegrityDataConfig),
      kid,
      rsaJwsSigner)
      .serialize();
  }

  private static JWTClaimsSet buildAgidJwtSignatureClaims(String digest, PuIntegrityDataConfig puIntegrityDataConfig) {
    String clientId = puIntegrityDataConfig.getClientId();
    long currentMillis = System.currentTimeMillis();
    long expirationMillis = currentMillis + (puIntegrityDataConfig.getExpirationMinutes() * 60 * 1000);

    return new JWTClaimsSet.Builder()
      .jwtID(UUID.randomUUID().toString())
      .issuer(clientId)
      .subject(clientId)
      .audience(puIntegrityDataConfig.getAudience())
      .issueTime(new Date(currentMillis))
      .expirationTime(new Date(expirationMillis))
      .claim("signed_headers", List.of(
          Map.of("digest", digest),
          Map.of(CONTENT_ENCODING_LOWERCASE, StandardCharsets.UTF_8.name()),
          Map.of(CONTENT_TYPE_LOWERCASE, MediaType.APPLICATION_JSON_VALUE)
        )
      )
      .build();
  }
}
