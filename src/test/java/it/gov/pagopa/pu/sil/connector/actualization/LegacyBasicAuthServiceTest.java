package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyBasicAuthClient;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LegacyBasicAuthServiceTest {
  @Mock
  private LegacyBasicAuthClient legacyBasicAuthClientMock;

  private LegacyBasicAuthService legacyBasicAuthService;

  @BeforeEach
  void setUp() {
    legacyBasicAuthService = new LegacyBasicAuthServiceImpl(legacyBasicAuthClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(legacyBasicAuthClientMock);
  }

  @Test
  void whenLoginThenReturnToken() {
    // Given
    String authUrl = "http://example.com";
    Credentials credentials = new Credentials()
      .username("testUser").password("testPassword");
    Token expectedToken = new Token();
    RegistryContextData contextData = mock(RegistryContextData.class);

    Mockito.when(legacyBasicAuthClientMock.login(contextData, credentials, authUrl))
           .thenReturn(expectedToken);

    // When
    Token result = legacyBasicAuthService.login(contextData, credentials, authUrl);

    // Then
    assertSame(expectedToken, result);
  }
}
