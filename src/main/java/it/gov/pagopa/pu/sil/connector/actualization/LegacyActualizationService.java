package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;

public interface LegacyActualizationService {
  ActualizationResultDTO actualization(String accessToken, String serviceUrl, Pagamento pagamento);
}
