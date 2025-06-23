package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyBasicAuthClient {
  private final ActualizationApisHolder actualizationApisHolder;

  public LegacyBasicAuthClient(ActualizationApisHolder actualizationApisHolder) {
    this.actualizationApisHolder = actualizationApisHolder;
  }

  public Token login(Credentials credentials, String authUrl) {
    return actualizationApisHolder.getAmountUpdatesLegacyApi(null, authUrl)
        .login(credentials);
  }
}
