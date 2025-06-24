package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyBasicAuthService;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyBasicAuthService {
  private final Integer expirationTimeInSeconds;
  private final LegacyBasicAuthService legacyBasicAuthService;

  public SilLegacyBasicAuthService(
    @Value("rest.auth.expiration-timeout-seconds.default") Integer expirationTimeInSeconds,
    LegacyBasicAuthService legacyBasicAuthService) {
      this.expirationTimeInSeconds = expirationTimeInSeconds;
      this.legacyBasicAuthService = legacyBasicAuthService;
  }

  public AccessToken authenticate(SilServiceLegacyBasicAuthConfig config) {
    // TODO: transform the Credentials fields properly https://pagopa.atlassian.net/browse/P4ADEV-3126
    Token token = legacyBasicAuthService.login(
      Credentials.builder()
        .username(String.valueOf(config.getUser()))
        .password(String.valueOf(config.getPsw()))
        .build(),
      config.getAuthUrl()
    );
    return AccessToken.builder()
      .accessToken(token.getToken())
      .expiresIn(expirationTimeInSeconds)
      .build();
  }
}
