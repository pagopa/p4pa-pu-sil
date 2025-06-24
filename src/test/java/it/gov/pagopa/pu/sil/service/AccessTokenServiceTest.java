package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

  @Test
  void givenExpiredCacheWhenGetAccessTokenThenInvokeAndCache(){
    // Given
    AccessToken expectedResult = AccessToken.builder()
      .expiresIn(5)
      .accessToken("ACCESSTOKEN")
      .tokenType("TOKENTYPE")
      .build();

    // When
    configureAndInvoke(expectedResult);

    // Then
    Mockito.verify(authAccessTokenFacadeMock, Mockito.times(2))
      .retrieveAccessToken(Mockito.any(), Mockito.any());
  }

  private void configureAndInvoke(AccessToken expectedResult) {
    // Given
    UserInfo userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(
      new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("ROLE_USER")),
      new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of("ROLE_ADMIN"))
    ));

    OrgSilService orgSilService = mock(OrgSilService.class);
    Mockito.when(authAccessTokenFacadeMock.retrieveAccessToken(orgSilService, userInfo))
      .thenReturn(expectedResult);

    // When
    AccessToken result1 = service.getAccessToken(orgSilService, userInfo);
    AccessToken result2 = service.getAccessToken(orgSilService, userInfo);

    // Then
    Assertions.assertSame(expectedResult, result1);
    Assertions.assertSame(expectedResult, result2);
  }
}
