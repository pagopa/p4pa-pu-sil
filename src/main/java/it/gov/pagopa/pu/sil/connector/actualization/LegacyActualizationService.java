package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;

public interface LegacyActualizationService {
  Token login(Credentials credentials, String authUrl);
  AmountUpdatesDTO actualization(String accessToken, String serviceUrl, Pagamento pagamento);
}
