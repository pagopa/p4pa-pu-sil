package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.organization.dto.generated.JwtAlgorithm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

class AlgorithmResolverServiceTest {
  private AlgorithmResolverService service = new AlgorithmResolverService();

  @ParameterizedTest
  @ValueSource( strings = {
    "RS256",
    "RS384",
    "RS512",
    "ES256",
    "ES384",
    "ES512",
    "HS256",
    "HS384",
    "HS512",
  })
  void givenAccessWhenBuildThenUnsupportedOperationException(String algorithm) {
    // Given
    byte[] signingKey = "signingKey".getBytes(StandardCharsets.UTF_8);

    // When  Then
    Assertions.assertDoesNotThrow(
      () -> service.resolveAlgorithm(JwtAlgorithm.valueOf(algorithm), signingKey));
  }

  @Test
  void givenAccessWhenBuildThenIllegalArgumentExceptionException() {
    // Given
    byte[] signingKey = "signingKey".getBytes(StandardCharsets.UTF_8);

    // When Then
    Assertions.assertThrows(IllegalArgumentException.class,
      () ->service.resolveAlgorithm(null, signingKey));
  }
}
