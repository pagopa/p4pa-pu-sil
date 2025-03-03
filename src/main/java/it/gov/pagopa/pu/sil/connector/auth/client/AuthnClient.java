package it.gov.pagopa.pu.sil.connector.auth.client;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.auth.config.AuthApisHolder;
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
    return authApisHolder.getAuthnApi(accessToken)
      .getUserInfo();
  }

}
