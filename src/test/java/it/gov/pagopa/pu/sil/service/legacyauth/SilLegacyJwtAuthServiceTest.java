package it.gov.pagopa.pu.sil.service.legacyauth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.JwtAlgorithm;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

class SilLegacyJwtAuthServiceTest {
  private static final Integer EXPIRATION_TIME_IN_SECONDS = 300;
  private SilLegacyJwtAuthService service;

  @BeforeEach
  void setUp() {
    service = new SilLegacyJwtAuthService(EXPIRATION_TIME_IN_SECONDS);
  }

  @ParameterizedTest
  @CsvSource({
    "HS256",
    "HS384",
    "HS512"
  })
  void givenAccessWhenBuildThenOk(String algorithm) {
    // Given
    byte[] signingKey = "signingKey".getBytes(StandardCharsets.UTF_8);
    SilServiceLegacyJwtAuthConfig authConfig = new SilServiceLegacyJwtAuthConfig()
      .kid("KEY_ID")
      .subject("SUBJECT")
      .issuer("ISSUER")
      .algorithm(JwtAlgorithm.valueOf(algorithm))
      .signingKey(signingKey);

    // When
    AccessToken result = service.authenticate(authConfig);

    // Then
    Assertions.assertEquals("bearer", result.getTokenType());
    Assertions.assertEquals(EXPIRATION_TIME_IN_SECONDS, result.getExpiresIn());

    DecodedJWT decodedAccessToken = JWT.decode(result.getAccessToken());
    String decodedHeader = new String(Base64.getDecoder().decode(decodedAccessToken.getHeader()));
    String decodedPayload = new String(Base64.getDecoder().decode(decodedAccessToken.getPayload()));

    Assertions.assertEquals(String.format("{\"kid\":\"KEY_ID\",\"typ\":\"at+JWT\",\"alg\":\"%s\"}", algorithm), decodedHeader);
    Assertions.assertEquals(Long.valueOf(EXPIRATION_TIME_IN_SECONDS), (decodedAccessToken.getExpiresAtAsInstant().toEpochMilli() - decodedAccessToken.getIssuedAtAsInstant().toEpochMilli()) / 1_000);
    Assertions.assertTrue(Pattern.compile("\\{\"typ\":\"bearer\",\"iss\":\"ISSUER\",\"jti\":\"[0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12}\",\"sub\":\"SUBJECT\",\"iat\":[0-9]+,\"exp\":[0-9]+}")
      .matcher(decodedPayload).matches(), "Payload not matches requested pattern: " + decodedPayload);
  }

  @ParameterizedTest
  @CsvSource({
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512"
  })
  void givenAccessWhenBuildThenUnsupportedOperationException(String algorithm) {
    // Given
    byte[] signingKey = "signingKey".getBytes(StandardCharsets.UTF_8);
    SilServiceLegacyJwtAuthConfig authConfig = new SilServiceLegacyJwtAuthConfig()
      .kid("KEY_ID")
      .subject("SUBJECT")
      .issuer("ISSUER")
      .algorithm(JwtAlgorithm.valueOf(algorithm))
      .signingKey(signingKey);

    // When Then
    Assertions.assertThrows(UnsupportedOperationException.class,
      () ->service.authenticate(authConfig));
  }

  @Test
  void givenAccessWhenBuildThenIllegalArgumentExceptionException() {
    // Given
    byte[] signingKey = "signingKey".getBytes(StandardCharsets.UTF_8);
    SilServiceLegacyJwtAuthConfig authConfig = new SilServiceLegacyJwtAuthConfig()
      .kid("KEY_ID")
      .subject("SUBJECT")
      .issuer("ISSUER")
      .signingKey(signingKey);

    // When Then
    Assertions.assertThrows(IllegalArgumentException.class,
      () ->service.authenticate(authConfig));
  }
}
