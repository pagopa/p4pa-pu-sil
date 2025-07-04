package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyBasicAuthService;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SilLegacyBasicAuthServiceTest {
  @Mock
  private LegacyBasicAuthService legacyBasicAuthService;
  private static final Integer EXPIRATION_TIME = 300;
  private SilLegacyBasicAuthService service;

  @BeforeEach
  void setUp() {
    service = new SilLegacyBasicAuthService(EXPIRATION_TIME, legacyBasicAuthService);
  }

  @Test
  void whenCallLegacyBasicAuthServiceThenReturnToken() {
    byte[] user = "user".getBytes();
    byte[] psw = "psw".getBytes();
    String authUrl = "http://auth.url";
    SilServiceLegacyBasicAuthConfig config = new SilServiceLegacyBasicAuthConfig()
      .user(user)
      .psw(psw)
      .authUrl(authUrl);
    Credentials credentials = Credentials.builder()
      .username(String.valueOf(user))
      .password(String.valueOf(psw))
      .build();

    Token expectedToken = new Token()
      .token("accessToken")
      .esito(Token.EsitoEnum.OK);

    RegistryContextData contextData = mock(RegistryContextData.class);

    when(legacyBasicAuthService.login(contextData, credentials, authUrl)).thenReturn(expectedToken);

    AccessToken result = service.authenticate(contextData, config);

    assertSame(expectedToken.getToken(), result.getAccessToken());
    assertEquals(EXPIRATION_TIME, result.getExpiresIn());
    verify(legacyBasicAuthService).login(contextData, credentials, authUrl);
  }
}
