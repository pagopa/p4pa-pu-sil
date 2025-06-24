package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    String clientId = "clientId";
    when(orgSilService.getFlagLegacy()).thenReturn(flagLegacy);
    if (flagLegacy) {
      when(silLegacyAuthFacadeService.authenticate(any())).thenReturn(accessToken);
    } else {
      when(authnClient.postToken(eq(clientId), any(), any(), any(), any(), any(), any())).thenReturn(accessToken);
    }

    AccessToken result = authAccessTokenFacade.retrieveAccessToken(orgSilService, clientId);
    assertEquals(accessToken, result);

    if (flagLegacy) {
      verify(silLegacyAuthFacadeService).authenticate(any());
      verify(authnClient, never()).postToken(any(), any(), any(), any(), any(), any(), any());
    } else {
      verify(authnClient).postToken(eq(clientId), any(), any(), any(), any(), any(), any());
      verify(silLegacyAuthFacadeService, never()).authenticate(any());
    }
  }
}

