package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceRequestBodyAuthConfig;
import it.gov.pagopa.pu.sil.dto.LegacyTokenDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
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
    SilServiceLegacyBasicAuthConfig config = mock(SilServiceLegacyBasicAuthConfig.class);
    LegacyTokenDTO expectedToken = mock(LegacyTokenDTO.class);
    when(basicAuthServiceMock.authenticate(config)).thenReturn(expectedToken);
    LegacyTokenDTO result = facadeService.authenticate(config);
    assertSame(expectedToken, result);
    verify(basicAuthServiceMock).authenticate(config);
    verifyNoInteractions(jwtAuthServiceMock);
  }

  @Test
  void authenticate_withJwtAuthConfig_delegatesToJwtAuthService() {
    SilServiceLegacyJwtAuthConfig config = mock(SilServiceLegacyJwtAuthConfig.class);
    LegacyTokenDTO expectedToken = mock(LegacyTokenDTO.class);
    when(jwtAuthServiceMock.authenticate(config)).thenReturn(expectedToken);
    LegacyTokenDTO result = facadeService.authenticate(config);
    assertSame(expectedToken, result);
    verify(jwtAuthServiceMock).authenticate(config);
    verifyNoInteractions(basicAuthServiceMock);
  }

  @Test
  void authenticate_withUnsupportedConfig_throwsException() {
    OrgSilServiceRequestBodyAuthConfig unsupportedConfig = mock(OrgSilServiceRequestBodyAuthConfig.class);
    assertThrows(IllegalArgumentException.class, () -> facadeService.authenticate(unsupportedConfig));
    verifyNoInteractions(basicAuthServiceMock, jwtAuthServiceMock);
  }
}
