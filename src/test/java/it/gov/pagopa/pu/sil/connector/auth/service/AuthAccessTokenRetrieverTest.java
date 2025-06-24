package it.gov.pagopa.pu.sil.connector.auth.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class AuthAccessTokenRetrieverTest {

  private static final String CLIENTSECRET = "clientsecret";

  @Mock
  private AuthnClient authnClientMock;
  @Mock
  private SilLegacyAuthFacadeService silLegacyAuthFacadeServiceMock;

  private AuthAccessTokenRetriever accessTokenRetriever;

  @BeforeEach
  void init(){
    accessTokenRetriever = new AuthAccessTokenRetriever(
      authnClientMock, silLegacyAuthFacadeServiceMock, CLIENTSECRET);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      authnClientMock
    );
  }

  @ParameterizedTest
  @CsvSource({
    "false,10,1",
    "true,10,1",
    "false,5,2",
    "true,5,2"
  })
  void testAccessTokenRetrieval(boolean legacyFlag, int expiresIn, int expectedInvocations) {
    String orgIpaCode = "ORGIPACODE";
    OrgSilService orgSilService = new OrgSilService();
    orgSilService.setFlagLegacy(legacyFlag);
    AccessToken expectedResult = AccessToken.builder()
      .expiresIn(expiresIn)
      .accessToken("ACCESSTOKEN")
      .tokenType("TOKENTYPE")
      .build();
    if (legacyFlag) {
      Mockito.when(silLegacyAuthFacadeServiceMock.authenticate(Mockito.any()))
        .thenReturn(expectedResult);
    } else {
      Mockito.when(authnClientMock.postToken("piattaforma-unitaria_" + orgIpaCode, "client_credentials", "openid", null, null, null, CLIENTSECRET))
        .thenReturn(expectedResult);
    }
    AccessToken result1 = accessTokenRetriever.getAccessToken(orgSilService, orgIpaCode);
    AccessToken result2 = accessTokenRetriever.getAccessToken(orgSilService, orgIpaCode);
    Assertions.assertSame(expectedResult, result1);
    Assertions.assertSame(expectedResult, result2);
    if (legacyFlag) {
      Mockito.verify(silLegacyAuthFacadeServiceMock, Mockito.times(expectedInvocations)).authenticate(Mockito.any());
    } else {
      Mockito.verify(authnClientMock, Mockito.times(expectedInvocations)).postToken(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
  }
}
