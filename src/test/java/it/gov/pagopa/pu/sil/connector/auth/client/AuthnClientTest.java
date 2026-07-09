package it.gov.pagopa.pu.sil.connector.auth.client;

import it.gov.pagopa.pu.auth.controller.generated.AuthnApi;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.LimitedTokenRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.auth.config.AuthApisHolder;
import it.gov.pagopa.pu.sil.exception.InvalidAccessTokenException;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.co.jemos.podam.api.PodamFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthnClientTest {

  @Mock
  private AuthApisHolder authApisHolderMock;
  @Mock
  private AuthnApi authnApiMock;

  private AuthnClient authnClient;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    authnClient = new AuthnClient(authApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      authApisHolderMock
    );
  }

  @Test
  void whenGetUserInfoThenInvokeWithAccessToken() {
    String accessToken = "ACCESSTOKEN";
    UserInfo expectedResult = new UserInfo();

    when(authApisHolderMock.getAuthnApi(accessToken))
      .thenReturn(authnApiMock);
    when(authnApiMock.getUserInfo())
      .thenReturn(expectedResult);

    UserInfo result = authnClient.getUserInfo(accessToken);

    assertSame(expectedResult, result);
  }

  @Test
  void givenUnauthorizedExceptionWhenGetUserInfoThenThrowInvalidAccessTokenException() {
    String accessToken = "ACCESSTOKEN";
    String bodyMessage = "bodyMessage";

    when(authApisHolderMock.getAuthnApi(accessToken))
      .thenReturn(authnApiMock);
    when(authnApiMock.getUserInfo())
      .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, bodyMessage.getBytes(), null));

    InvalidAccessTokenException exception = Assertions.assertThrows(InvalidAccessTokenException.class, () -> authnClient.getUserInfo(accessToken));

    assertEquals(bodyMessage, exception.getMessage());
  }

  @Test
  void whenPostLimitedTokenThenInvokeWithAccessToken() {
    String accessToken = "ACCESSTOKEN";
    LimitedTokenRequest limitedTokenRequest = new LimitedTokenRequest();
    AccessToken expectedResult = new AccessToken();

    when(authApisHolderMock.getAuthnApi(accessToken))
      .thenReturn(authnApiMock);
    when(authnApiMock.postLimitedToken(limitedTokenRequest))
      .thenReturn(expectedResult);

    AccessToken result = authnClient.postLimitedToken(limitedTokenRequest, accessToken);

    assertSame(expectedResult, result);
  }

  @Test
  void givenParamsWhenGetTokenThenReturnAccessToken(){
    //given
    String grantType = "grantType";
    String scope = "scope";
    String clientId= "clientId";
    String subjectToken = "subjectToken";
    String subjectIssuer = "subjectIssuer";
    String subjectTokenType = "subjectTokenType";
    String clientSecret = "clientSecret";

    AccessToken expectedResult = podamFactory.manufacturePojo(AccessToken.class);
    when(authApisHolderMock.getAuthnApi(null)).thenReturn(authnApiMock);
    when(authnApiMock.postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret, null)).thenReturn(expectedResult);

    //when
    AccessToken result = authnClient.postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret);

    //then
    assertNotNull(result);
    assertEquals(expectedResult, result);
  }
}
