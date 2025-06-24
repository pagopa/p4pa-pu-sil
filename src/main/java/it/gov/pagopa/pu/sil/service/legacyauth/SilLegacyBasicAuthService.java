package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyBasicAuthService;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.dto.LegacyTokenDTO;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyBasicAuthService {
  private final LegacyBasicAuthService legacyBasicAuthService;

  public SilLegacyBasicAuthService(LegacyBasicAuthService legacyBasicAuthService) {
    this.legacyBasicAuthService = legacyBasicAuthService;
  }

  public LegacyTokenDTO authenticate(SilServiceLegacyBasicAuthConfig config) {
    // TODO: transform the Credentials fields properly https://pagopa.atlassian.net/browse/P4ADEV-3126
    Token token = legacyBasicAuthService.login(
      Credentials.builder()
        .username(String.valueOf(config.getUser()))
        .password(String.valueOf(config.getPsw()))
        .build(),
      config.getAuthUrl()
    );
    return LegacyTokenDTO.builder()
      .token(token.getToken())
      .build();
  }
}
