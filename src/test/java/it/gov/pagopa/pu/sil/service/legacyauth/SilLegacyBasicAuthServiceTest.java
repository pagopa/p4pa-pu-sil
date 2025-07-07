package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfigDTO;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyBasicAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    String user = "user";
    String psw = "psw";
    String authUrl = "http://auth.url";
    SilServiceLegacyBasicAuthConfigDTO config = new SilServiceLegacyBasicAuthConfigDTO()
      .user(user)
      .psw(psw)
      .authUrl(authUrl);
    Credentials credentials = Credentials.builder()
      .username(user)
      .password(psw)
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
