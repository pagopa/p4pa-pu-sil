package it.gov.pagopa.pu.sil.connector.auth.client;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.LimitedTokenRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.auth.config.AuthApisHolder;
import it.gov.pagopa.pu.sil.exception.InvalidAccessTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

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
    } catch (HttpClientErrorException.Unauthorized e) {
      throw new InvalidAccessTokenException(e.getResponseBodyAsString());
    }
  }

  public AccessToken postLimitedToken(LimitedTokenRequest limitedTokenRequest, String accessToken) {
    return authApisHolder.getAuthnApi(accessToken)
      .postLimitedToken(limitedTokenRequest);
  }

  public AccessToken postToken(String clientId, String grantType, String scope, String subjectToken, String subjectIssuer, String subjectTokenType, String clientSecret) {
    return authApisHolder.getAuthnApi(null)
      .postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret);
  }
}
