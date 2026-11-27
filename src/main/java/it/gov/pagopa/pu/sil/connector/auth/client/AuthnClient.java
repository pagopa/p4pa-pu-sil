package it.gov.pagopa.pu.sil.connector.auth.client;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.LimitedTokenRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.auth.config.AuthApisHolder;
import it.gov.pagopa.pu.sil.exception.InvalidAccessTokenException;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthnClient {

  private final AuthApisHolder authApisHolder;

  public AuthnClient(AuthApisHolder authApisHolder) {
    this.authApisHolder = authApisHolder;
  }

  public UserInfo getUserInfo(String accessToken) {
    try {
      return authApisHolder.getAuthnApi(accessToken)
        .getUserInfo();
    } catch (RestInvokeNotAuthorizedException e) {
      throw new InvalidAccessTokenException(e.getMessage());
    }
  }

  public AccessToken postLimitedToken(LimitedTokenRequest limitedTokenRequest, String accessToken) {
    return authApisHolder.getAuthnApi(accessToken)
      .postLimitedToken(limitedTokenRequest);
  }

  public AccessToken postToken(String clientId, String grantType, String scope, String subjectToken, String subjectIssuer, String subjectTokenType, String clientSecret) {
    return authApisHolder.getAuthnApi(null)
      .postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret, null);
  }
}
