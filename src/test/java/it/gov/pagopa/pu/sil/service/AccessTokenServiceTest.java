package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {

  @Mock
  private SilLegacyAuthFacadeService silLegacyAuthFacadeServiceMock;

  private AccessTokenService service;

  @BeforeEach
  void init(){
    service = new AccessTokenService(silLegacyAuthFacadeServiceMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(silLegacyAuthFacadeServiceMock);
  }

  @Test
  void givenEmptyCacheWhenGetAccessTokenThenInvokeAndCache(){
    // Given
    AccessToken expectedResult = AccessToken.builder()
      .expiresIn(10)
      .accessToken("ACCESSTOKEN")
      .tokenType("TOKENTYPE")
      .build();

    // When
    configureAndInvoke(expectedResult);

    // Then
    Mockito.verify(silLegacyAuthFacadeServiceMock, Mockito.times(1))
      .authenticate(Mockito.any());
  }

  @Test
  void givenLoggedUserAccessTokenWhenGetAccessTokenThenReturnToken() {
    // Given
    AccessToken expectedResult = AccessToken.builder()
      .expiresIn(10)
      .accessToken("ACCESSTOKEN")
      .tokenType("TOKENTYPE")
      .build();

    OrgSilService orgSilService = new OrgSilService().flagLegacy(false);
    AccessToken result = service.getAccessToken(orgSilService, expectedResult);

    Assertions.assertSame(expectedResult, result);
  }

  private void configureAndInvoke(AccessToken expectedResult) {
    // Given
    AccessToken accessToken = new AccessToken()
        .accessToken("ACCESSTOKEN")
        .tokenType("TOKENTYPE")
        .expiresIn(10);

    OrgSilService orgSilService = new OrgSilService().flagLegacy(true);
    Mockito.when(silLegacyAuthFacadeServiceMock.authenticate(orgSilService.getAuthConfig()))
      .thenReturn(expectedResult);

    // When
    AccessToken result1 = service.getAccessToken(orgSilService, accessToken);
    AccessToken result2 = service.getAccessToken(orgSilService, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result1);
    Assertions.assertSame(expectedResult, result2);
  }
}
