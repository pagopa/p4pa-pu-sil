package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyActualizationClient {
  private final ActualizationApisHolder actualizationApisHolder;
  private final RegistryLogger registryLogger;

  public LegacyActualizationClient(ActualizationApisHolder actualizationApisHolder,
                                   RegistryLogger registryLogger) {
    this.actualizationApisHolder = actualizationApisHolder;
    this.registryLogger = registryLogger;
  }

  public PagamentoAggiornato actualization(RegistryContextData contextData, String accessToken, String serviceUrl, Pagamento pagamento) {
    return registryLogger.execute(
      contextData,
      pagamento,
      () -> Triple.of(actualizationApisHolder.getAmountUpdatesLegacyApi(accessToken, serviceUrl.replace("/notification-price", ""))
          .attualizzazione(pagamento),
        null,
        RegistryOutcome.OK
      ),
      null
    );
  }
}
