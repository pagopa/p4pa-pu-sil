package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthAccessTokenFacadeTest {

  @Mock
  private AuthnClient authnClient;
  @Mock
  private SilLegacyAuthFacadeService silLegacyAuthFacadeService;
  @Mock
  private OrgSilService orgSilService;
  @Mock
  private AccessToken accessToken;

  @InjectMocks
  private AuthAccessTokenFacade authAccessTokenFacade;

  @BeforeEach
  void setUp() {
    authAccessTokenFacade = new AuthAccessTokenFacade(
      authnClient, silLegacyAuthFacadeService, "dummySecret");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testRetrieveAccessToken(boolean flagLegacy) {
    UserInfo userInfo = mock(UserInfo.class);
    when(orgSilService.getFlagLegacy()).thenReturn(flagLegacy);
    if (flagLegacy) {
      when(silLegacyAuthFacadeService.authenticate(any())).thenReturn(accessToken);

      AccessToken result = authAccessTokenFacade.retrieveAccessToken(orgSilService, userInfo);
      assertEquals(accessToken, result);

      verify(silLegacyAuthFacadeService).authenticate(any());
      verify(authnClient, never()).postToken(any(), any(), any(), any(), any(), any(), any());
    } else {
      try (MockedStatic<AuthorizationService> utilities = mockStatic(AuthorizationService.class)) {
        when(orgSilService.getOrganizationId()).thenReturn(123L);
        utilities.when(() -> AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, 123L)).thenReturn("IPA_CODE");
        String expectedClientId = "piattaforma-unitaria_IPA_CODE";
        when(authnClient.postToken(eq(expectedClientId), any(), any(), any(), any(), any(), any())).thenReturn(accessToken);

        AccessToken result = authAccessTokenFacade.retrieveAccessToken(orgSilService, userInfo);
        assertEquals(accessToken, result);

        verify(authnClient).postToken(eq(expectedClientId), any(), any(), any(), any(), any(), any());
        verify(silLegacyAuthFacadeService, never()).authenticate(any());
      }
    }
  }
}
