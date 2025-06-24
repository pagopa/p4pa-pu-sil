package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthAccessTokenFacadeTest {

  @Mock
  private SilLegacyAuthFacadeService silLegacyAuthFacadeServiceMock;

  private AuthAccessTokenFacade authAccessTokenFacade;

  @BeforeEach
  void setUp() {
    authAccessTokenFacade = new AuthAccessTokenFacade(silLegacyAuthFacadeServiceMock);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testRetrieveAccessToken(boolean flagLegacy) {
    AccessToken accessToken = new AccessToken()
      .accessToken("testAccessToken")
      .expiresIn(300)
      .tokenType("Bearer");
    OrgSilService orgSilService = new OrgSilService()
      .flagLegacy(flagLegacy);

    if (flagLegacy) {
      when(silLegacyAuthFacadeServiceMock.authenticate(any())).thenReturn(accessToken);

      AccessToken result = authAccessTokenFacade.retrieveAccessToken(orgSilService, accessToken);
      assertEquals(accessToken, result);

      verify(silLegacyAuthFacadeServiceMock).authenticate(any());
    } else {
      AccessToken result = authAccessTokenFacade.retrieveAccessToken(orgSilService, accessToken);
      assertEquals(accessToken, result);

      verify(silLegacyAuthFacadeServiceMock, never()).authenticate(any());
    }
  }
}

