package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;

public interface LegacyActualizationService {
  AmountUpdatesDTO actualization(String accessToken, String serviceUrl, Pagamento pagamento);
}
