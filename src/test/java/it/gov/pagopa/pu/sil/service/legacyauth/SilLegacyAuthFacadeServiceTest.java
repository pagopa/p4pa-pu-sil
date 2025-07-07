package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTOAuthConfig;
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
    SilServiceLegacyBasicAuthConfigDTO config = mock(SilServiceLegacyBasicAuthConfigDTO.class);
    AccessToken expectedToken = mock(AccessToken.class);
    when(basicAuthServiceMock.authenticate(config)).thenReturn(expectedToken);
    AccessToken result = facadeService.authenticate(config);
    assertSame(expectedToken, result);
    verify(basicAuthServiceMock).authenticate(config);
    verifyNoInteractions(jwtAuthServiceMock);
  }

  @Test
  void authenticate_withJwtAuthConfig_delegatesToJwtAuthService() {
    SilServiceLegacyJwtAuthConfigDTO config = mock(SilServiceLegacyJwtAuthConfigDTO.class);
    AccessToken expectedToken = mock(AccessToken.class);
    when(jwtAuthServiceMock.authenticate(config)).thenReturn(expectedToken);
    AccessToken result = facadeService.authenticate(config);
    assertSame(expectedToken, result);
    verify(jwtAuthServiceMock).authenticate(config);
    verifyNoInteractions(basicAuthServiceMock);
  }

  @Test
  void authenticate_withUnsupportedConfig_throwsException() {
    OrgSilServiceDTOAuthConfig unsupportedConfig = mock(OrgSilServiceDTOAuthConfig.class);
    assertThrows(IllegalArgumentException.class, () -> facadeService.authenticate(unsupportedConfig));
    verifyNoInteractions(basicAuthServiceMock, jwtAuthServiceMock);
  }
}
