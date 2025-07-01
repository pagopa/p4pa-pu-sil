package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.organization.dto.generated.JwtAlgorithm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;

class AlgorithmResolverServiceTest {

  private AlgorithmResolverService service = new AlgorithmResolverService();

  @ParameterizedTest
  @ValueSource( strings = {
    "HS256",
    "HS384",
    "HS512",
  })
  void givenHMACThenReturnAlgorithm(String algorithm) {
    // When  Then
    Assertions.assertDoesNotThrow(
        () -> service.resolveAlgorithm(JwtAlgorithm.valueOf(algorithm), "signingKey"));
  }

  @ParameterizedTest
  @CsvSource({
    "ES256, 256",
    "ES384, 384",
    "ES512, 521",
  })
  void givenECDSAThenReturnAlgorithm(String algorithm, Integer size) throws Exception {
    // Given
    byte[] signingKey = keyGenerator("EC", size);
    String keyValue = Base64.getEncoder().encodeToString(signingKey);
    // When  Then
    Assertions.assertDoesNotThrow(
      () -> service.resolveAlgorithm(JwtAlgorithm.valueOf(algorithm), keyValue));
  }

  @ParameterizedTest
  @ValueSource( strings = {
    "RS256",
    "RS384",
    "RS512",
  })
  void givenRSAThenReturnAlgorithm(String algorithm) throws Exception {
    // Given
    byte[] signingKey = keyGenerator("RSA", 2048);
    String keyValue = Base64.getEncoder().encodeToString(signingKey);
    // When  Then
    Assertions.assertDoesNotThrow(
        () -> service.resolveAlgorithm(JwtAlgorithm.valueOf(algorithm), keyValue));
  }

  @Test
  void givenNoAlgorithmIllegalArgumentExceptionException() {
    // When Then
    Assertions.assertThrows(IllegalArgumentException.class,
      () ->service.resolveAlgorithm(null, "signingKey"));
  }

  private static byte[] keyGenerator(String algorithm, Integer size) throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
    keyGen.initialize(size);
    KeyPair pair = keyGen.generateKeyPair();
    PrivateKey privateKey = pair.getPrivate();
    return privateKey.getEncoded();
  }
}
