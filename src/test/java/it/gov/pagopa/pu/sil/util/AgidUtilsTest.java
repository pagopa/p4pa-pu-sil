package it.gov.pagopa.pu.sil.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.crypto.RSASSASigner;
import it.gov.pagopa.pu.sil.config.rest.agid.PuIntegrityDataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonAssert;
import org.springframework.test.json.JsonCompareMode;

import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.Map;

public class AgidUtilsTest {

  public static RSASSASigner signer;

  static {
    try {
      signer = new RSASSASigner(CertUtils.<RSAPrivateKey>pemKey2PrivateKey("RSA", CertUtilsTest.PRIVATE_KEY));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private final PuIntegrityDataConfig puIntegrityDataConfig = PuIntegrityDataConfig.builder()
    .issuer("ISSUER")
    .expirationMinutes(1L)
    .publicKey(CertUtilsTest.PUBLIC_KEY)
    .build();

  @Test
  void whenBuildAgidJwtSignatureThenOk() {
    // Given
    String digest = "DIGEST";

    // When
    String token = AgidUtils.buildAgidJwtSignature(
      digest,
      puIntegrityDataConfig,
      signer);

    // Then
    DecodedJWT decoded = JWT.decode(token);
    JsonAssert.comparator(JsonCompareMode.STRICT).assertIsMatch(
      Map.of(
        RegisteredClaims.JWT_ID, decoded.getClaim(RegisteredClaims.JWT_ID),
        RegisteredClaims.ISSUER, puIntegrityDataConfig.getIssuer(),
        RegisteredClaims.SUBJECT, puIntegrityDataConfig.getIssuer(),
        RegisteredClaims.ISSUED_AT, decoded.getClaim(RegisteredClaims.ISSUED_AT),
        RegisteredClaims.EXPIRES_AT, decoded.getClaim(RegisteredClaims.ISSUED_AT).asLong() + 60,
        "signed_headers", List.of(
          Map.of("digest", digest),
          Map.of("\"content-encoding\"", "\"UTF-8\""),
          Map.of("\"content-type\"", "\"application/json\"")
        )
      ).toString(),
      decoded.getClaims().toString());
  }

  @Test
  void whenBuildDigestThenOk() {
    Assertions.assertEquals(
      "SHA-256=f9SGUoWD/kZFYdz81VpXWA9SCqyEw0hZXvnSdwuRRG8=",
      AgidUtils.buildDigest("PROVA")
    );
  }
}
