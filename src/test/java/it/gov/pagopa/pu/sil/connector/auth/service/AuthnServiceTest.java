package it.gov.pagopa.pu.sil.connector.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthnServiceTest {
  @Mock
  private AuthnClient authnClient;
  private AuthnServiceImpl authnService;

  @BeforeEach
  void setUp() {
    authnService = new AuthnServiceImpl(authnClient);
  }

  @Test
  void whenPostTokenThenInvokeClient() {
    // Arrange
    String clientId = "clientId";
    String grantType = "grantType";
    String scope = "scope";
    String subjectToken = "subjectToken";
    String subjectIssuer = "subjectIssuer";
    String subjectTokenType = "subjectTokenType";
    String clientSecret = "clientSecret";
    AccessToken expectedToken = mock(AccessToken.class);
    when(authnClient.postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret))
      .thenReturn(expectedToken);

    // Act
    AccessToken result = authnService.postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret);

    // Assert
    assertEquals(expectedToken, result);
    verify(authnClient).postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret);
  }
}
