package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SilAccessTokenServiceTest {

  @Mock
  private SilLegacyAuthFacadeService silLegacyAuthFacadeServiceMock;

  private SilAccessTokenService service;

  @BeforeEach
  void init(){
    service = new SilAccessTokenService(silLegacyAuthFacadeServiceMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(silLegacyAuthFacadeServiceMock);
  }

  @Test
  void givenEmptyCacheWhenGetSilAccessTokenThenInvokeAndCache(){
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
  void givenLoggedUserAccessTokenWhenGetSilAccessTokenThenReturnToken() {
    // Given
    String token = "ACCESSTOKEN";

    OrgSilService orgSilService = new OrgSilService().flagLegacy(false);
    String result = service.getSilAccessToken(orgSilService, token);

    Assertions.assertSame(token, result);
  }

  private void configureAndInvoke(AccessToken expectedResult) {
    // Given
    String token = "ACCESSTOKEN";
    SilServiceLegacyBasicAuthConfig config = mock(SilServiceLegacyBasicAuthConfig.class);
    OrgSilService orgSilService = new OrgSilService()
      .orgSilServiceId(1L)
      .authConfig(config)
      .flagLegacy(true);
    Mockito.when(silLegacyAuthFacadeServiceMock.authenticate(orgSilService.getAuthConfig()))
      .thenReturn(expectedResult);

    // When
    String result1 = service.getSilAccessToken(orgSilService, token);
    String result2 = service.getSilAccessToken(orgSilService, token);

    // Then
    Assertions.assertSame(expectedResult.getAccessToken(), result1);
    Assertions.assertSame(expectedResult.getAccessToken(), result2);
  }
}
