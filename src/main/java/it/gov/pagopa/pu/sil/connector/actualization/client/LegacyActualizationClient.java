package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyActualizationClient {
  private final ActualizationApisHolder actualizationApisHolder;

  public LegacyActualizationClient(ActualizationApisHolder actualizationApisHolder) {
    this.actualizationApisHolder = actualizationApisHolder;
  }

  public PagamentoAggiornato actualization(String accessToken, String serviceUrl, Pagamento pagamento) {
    return actualizationApisHolder.getAmountUpdatesLegacyApi(accessToken, serviceUrl.replace("/notification-price", ""))
      .attualizzazione(pagamento);
  }
}
