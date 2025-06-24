package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
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
class AccessTokenServiceTest {

  @Mock
  private AuthAccessTokenFacade authAccessTokenFacadeMock;

  private AccessTokenService service;

  @BeforeEach
  void init(){
    service = new AccessTokenService(authAccessTokenFacadeMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(authAccessTokenFacadeMock);
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
    Mockito.verify(authAccessTokenFacadeMock, Mockito.times(1))
      .retrieveAccessToken(Mockito.any(), Mockito.any());
  }

  private void configureAndInvoke(AccessToken expectedResult) {
    // Given
    AccessToken accessToken = new AccessToken()
        .accessToken("ACCESSTOKEN")
        .tokenType("TOKENTYPE")
        .expiresIn(10);

    OrgSilService orgSilService = mock(OrgSilService.class);
    Mockito.when(authAccessTokenFacadeMock.retrieveAccessToken(orgSilService, accessToken))
      .thenReturn(expectedResult);

    // When
    AccessToken result1 = service.getAccessToken(orgSilService, accessToken);
    AccessToken result2 = service.getAccessToken(orgSilService, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result1);
    Assertions.assertSame(expectedResult, result2);
  }
}
