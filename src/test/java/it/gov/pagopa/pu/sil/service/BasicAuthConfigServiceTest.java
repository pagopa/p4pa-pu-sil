package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceRequestBodyAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyBasicAuthService;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.service.legacyauth.BasicAuthConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicAuthConfigServiceTest {
  @Mock
  private LegacyBasicAuthService legacyBasicAuthServiceMock;

  private BasicAuthConfigService service;

  @BeforeEach
  void setUp() {
    service = new BasicAuthConfigService(legacyBasicAuthServiceMock);
  }

  @Test
  void whenDoAuthenticationThenCallLegacyBasicAuthServiceAndReturnToken() {
    String authUrl = "http://auth.url";
    byte[] user = "user".getBytes();
    byte[] psw = "pass".getBytes();
    Credentials credentials = Credentials.builder()
      .username(String.valueOf(user))
      .password(String.valueOf(psw))
      .build();

    OrgSilServiceRequestBodyAuthConfig config = new
      SilServiceLegacyBasicAuthConfig()
      .authUrl(authUrl)
      .user(user)
      .psw(psw);
    OrgSilService orgSilService = new OrgSilService().authConfig(config);

    Token expectedToken = new Token();
    when(legacyBasicAuthServiceMock.login(credentials, authUrl)).thenReturn(expectedToken);

    Token result = service.getAuthConfig(orgSilService).doAuthentication();

    assertEquals(expectedToken, result);
  }
}

