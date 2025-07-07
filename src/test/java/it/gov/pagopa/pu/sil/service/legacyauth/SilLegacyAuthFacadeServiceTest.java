package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceRequestBodyAuthConfig;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
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
    RegistryContextData contextData = mock(RegistryContextData.class);
    when(basicAuthServiceMock.authenticate(contextData, config)).thenReturn(expectedToken);
    AccessToken result = facadeService.authenticate(contextData, config);
    assertSame(expectedToken, result);
    verify(basicAuthServiceMock).authenticate(contextData, config);
    verifyNoInteractions(jwtAuthServiceMock);
  }

  @Test
  void authenticate_withJwtAuthConfig_delegatesToJwtAuthService() {
    SilServiceLegacyJwtAuthConfigDTO config = mock(SilServiceLegacyJwtAuthConfigDTO.class);
    AccessToken expectedToken = mock(AccessToken.class);
    when(jwtAuthServiceMock.authenticate(config)).thenReturn(expectedToken);
    AccessToken result = facadeService.authenticate(new RegistryContextData(), config);
    assertSame(expectedToken, result);
    verify(jwtAuthServiceMock).authenticate(config);
    verifyNoInteractions(basicAuthServiceMock);
  }

  @Test
  void authenticate_withUnsupportedConfig_throwsException() {
    OrgSilServiceDTOAuthConfig unsupportedConfig = mock(OrgSilServiceDTOAuthConfig.class);
    RegistryContextData contextData = mock(RegistryContextData.class);
    assertThrows(IllegalArgumentException.class, () -> facadeService.authenticate(contextData, unsupportedConfig));
    verifyNoInteractions(basicAuthServiceMock, jwtAuthServiceMock);
  }
}
