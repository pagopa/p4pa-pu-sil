package it.gov.pagopa.pu.sil.connector.sil.actualization;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.sil.actualization.client.LegacyBasicAuthClient;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Credentials;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Token;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    String orgFiscalCode = "orgFiscalCode";
    String orgSilServiceName = "TestService";
    String nav = "1234567890";
    UserInfo loggedUser = mock(UserInfo.class);
    String authUrl = "http://example.com";
    Credentials credentials = new Credentials()
      .username("testUser").password("testPassword");
    Token expectedToken = new Token();

    when(legacyBasicAuthClientMock.login(orgFiscalCode, orgSilServiceName, nav, loggedUser, credentials, authUrl))
           .thenReturn(expectedToken);

    // When
    Token result = legacyBasicAuthService.login(orgFiscalCode, orgSilServiceName, nav, loggedUser, credentials, authUrl);

    // Then
    assertSame(expectedToken, result);
  }
}
