package it.gov.pagopa.pu.sil.connector.amountupdates;

import it.gov.pagopa.amountupdates.legacy.dto.generated.Credentials;
import it.gov.pagopa.amountupdates.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.amountupdates.client.AmountUpdatesLegacyClient;
import org.springframework.stereotype.Component;

@Component
public class AmountUpdatesLegacyComponentImpl implements AmountUpdatesLegacyComponent {
  private final AmountUpdatesLegacyClient amountUpdatesLegacyClient;

  public AmountUpdatesLegacyComponentImpl(AmountUpdatesLegacyClient amountUpdatesLegacyClient) {
    this.amountUpdatesLegacyClient = amountUpdatesLegacyClient;
  }

  @Override
  public Token login(Credentials credentials, String authUrl) {
    return amountUpdatesLegacyClient.login(credentials, authUrl);
  }
}
