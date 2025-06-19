package it.gov.pagopa.pu.sil.connector.amountupdates;

import it.gov.pagopa.amountupdates.legacy.dto.generated.Credentials;
import it.gov.pagopa.amountupdates.legacy.dto.generated.Token;

public interface AmountUpdatesLegacyComponent {
  Token login(Credentials credentials, String authUrl);
}
