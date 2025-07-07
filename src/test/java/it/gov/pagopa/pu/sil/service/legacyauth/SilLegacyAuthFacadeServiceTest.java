package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfigDTO;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SilLegacyAuthFacadeServiceTest {
  @Mock
  private SilLegacyBasicAuthService basicAuthServiceMock;
  @Mock
  private SilLegacyJwtAuthService jwtAuthServiceMock;

  private SilLegacyAuthFacadeService facadeService;

  @BeforeEach
  void setUp() {
    facadeService = new SilLegacyAuthFacadeService(basicAuthServiceMock, jwtAuthServiceMock);
  }

  @Test
  void authenticate_withBasicAuthConfig_delegatesToBasicAuthService() {
    String orgFiscalCode = "orgFiscalCode";
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    SilServiceLegacyBasicAuthConfigDTO config = mock(SilServiceLegacyBasicAuthConfigDTO.class);
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO()
      .applicationName("TestApp")
      .authConfig(config);
    AccessToken expectedToken = mock(AccessToken.class);

    when(basicAuthServiceMock.authenticate(orgFiscalCode, orgSilService.getApplicationName(), nav, loggedUser, config)).thenReturn(expectedToken);

    AccessToken result = facadeService.authenticate(orgFiscalCode, nav, loggedUser, orgSilService);

    assertSame(expectedToken, result);
    verify(basicAuthServiceMock).authenticate(orgFiscalCode, orgSilService.getApplicationName(), nav, loggedUser, config);
    verifyNoInteractions(jwtAuthServiceMock);
  }

  @Test
  void authenticate_withJwtAuthConfig_delegatesToJwtAuthService() {
    String orgFiscalCode = "orgFiscalCode";
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    SilServiceLegacyJwtAuthConfigDTO config = mock(SilServiceLegacyJwtAuthConfigDTO.class);
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO()
      .authConfig(config);
    AccessToken expectedToken = mock(AccessToken.class);

    when(jwtAuthServiceMock.authenticate(config)).thenReturn(expectedToken);

    AccessToken result = facadeService.authenticate(orgFiscalCode, nav, loggedUser, orgSilService);

    assertSame(expectedToken, result);
    verify(jwtAuthServiceMock).authenticate(config);
    verifyNoInteractions(basicAuthServiceMock);
  }

  @Test
  void authenticate_withUnsupportedConfig_throwsException() {
    String orgFiscalCode = "orgFiscalCode";
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO()
      .authConfig(null)
      .organizationId(1L)
      .orgSilServiceId(123L)
      .flagLegacy(true)
      .serviceUrl("http://service.url");

    assertThrows(IllegalArgumentException.class, () -> facadeService.authenticate(orgFiscalCode, nav, loggedUser, orgSilService));
    verifyNoInteractions(basicAuthServiceMock, jwtAuthServiceMock);
  }
}
