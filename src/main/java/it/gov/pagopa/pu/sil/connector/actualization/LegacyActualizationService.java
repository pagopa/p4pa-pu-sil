package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;

public interface LegacyActualizationService {
  ActualizationResultDTO actualization(RegistryContextData contextData, String accessToken, String serviceUrl, Pagamento pagamento);
}
