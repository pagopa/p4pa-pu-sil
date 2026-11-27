package it.gov.pagopa.pu.sil.service.outbound.legacyauth;

import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Credentials;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfigDTO;
import it.gov.pagopa.pu.sil.connector.sil.actualization.LegacyBasicAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyBasicAuthService {
  private final Integer expirationTimeInSeconds;
  private final LegacyBasicAuthService legacyBasicAuthService;

  public SilLegacyBasicAuthService(
    @Value("${legacy-auth.basic-legacy-expiration-seconds}") Integer expirationTimeInSeconds,
    LegacyBasicAuthService legacyBasicAuthService) {
      this.expirationTimeInSeconds = expirationTimeInSeconds;
      this.legacyBasicAuthService = legacyBasicAuthService;
  }

  public AccessToken authenticate(String orgFiscalCode, String orgSilServiceName, String nav, UserInfo loggedUser, SilServiceLegacyBasicAuthConfigDTO config) {
    Token token = legacyBasicAuthService.login(orgFiscalCode, orgSilServiceName, nav, loggedUser,
      Credentials.builder()
        .username(config.getUser())
        .password(config.getPsw())
        .build(),
      config.getAuthUrl()
    );
    return AccessToken.builder()
      .accessToken(token.getToken())
      .expiresIn(expirationTimeInSeconds)
      .tokenType("Bearer")
      .build();
  }
}
