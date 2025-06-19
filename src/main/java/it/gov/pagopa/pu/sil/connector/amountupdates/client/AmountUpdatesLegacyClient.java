package it.gov.pagopa.pu.sil.connector.amountupdates.client;

import it.gov.pagopa.amountupdates.legacy.dto.generated.Credentials;
import it.gov.pagopa.amountupdates.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.amountupdates.config.AmountUpdatesApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AmountUpdatesLegacyClient {
  private final AmountUpdatesApisHolder amountUpdatesApisHolder;

  public AmountUpdatesLegacyClient(AmountUpdatesApisHolder amountUpdatesApisHolder) {
    this.amountUpdatesApisHolder = amountUpdatesApisHolder;
  }

  public Token login(Credentials credentials, String authUrl) {
    return amountUpdatesApisHolder.getAmountUpdatesLegacyApi(null, authUrl)
        .login(credentials);
  }
}
