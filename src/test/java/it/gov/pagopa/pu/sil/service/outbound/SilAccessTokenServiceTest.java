package it.gov.pagopa.pu.sil.service.outbound;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfigDTO;
import it.gov.pagopa.pu.sil.service.outbound.legacyauth.SilLegacyAuthFacadeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

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
    verify(silLegacyAuthFacadeServiceMock, times(1))
      .authenticate(Mockito.anyString(), Mockito.anyString(), Mockito.any(UserInfo.class), Mockito.any(OrgSilServiceDTO.class));
  }

  @Test
  void givenLoggedUserAccessTokenWhenGetSilAccessTokenThenReturnToken() {
    // Given
    String token = "ACCESSTOKEN";
    String orgFiscalCode = "orgFiscalCode";
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);

    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO().flagLegacy(false);
    String result = service.getSilAccessToken(orgFiscalCode, nav, loggedUser, orgSilService, token);

    Assertions.assertSame(token, result);
  }

  private void configureAndInvoke(AccessToken expectedResult) {
    // Given
    String token = "ACCESSTOKEN";
    String orgFiscalCode = "orgFiscalCode";
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    SilServiceLegacyBasicAuthConfigDTO config = mock(SilServiceLegacyBasicAuthConfigDTO.class);
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO()
      .orgSilServiceId(1L)
      .authConfig(config)
      .flagLegacy(true);
    when(silLegacyAuthFacadeServiceMock.authenticate(orgFiscalCode, nav, loggedUser, orgSilService))
      .thenReturn(expectedResult);

    // When
    String result1 = service.getSilAccessToken(orgFiscalCode, nav, loggedUser, orgSilService, token);
    String result2 = service.getSilAccessToken(orgFiscalCode, nav, loggedUser, orgSilService, token);

    // Then
    Assertions.assertSame(expectedResult.getAccessToken(), result1);
    Assertions.assertSame(expectedResult.getAccessToken(), result2);
  }
}
