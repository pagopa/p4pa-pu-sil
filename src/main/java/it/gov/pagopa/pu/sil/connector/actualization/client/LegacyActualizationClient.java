package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.dto.generated.Error;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.dto.generated.PuSilErrorDTO;
import it.gov.pagopa.pu.sil.exception.ActualizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class LegacyActualizationClient {
  private final ActualizationApisHolder actualizationApisHolder;

  public LegacyActualizationClient(ActualizationApisHolder actualizationApisHolder) {
    this.actualizationApisHolder = actualizationApisHolder;
  }

  public PagamentoAggiornato actualization(String accessToken, String serviceUrl, Pagamento pagamento) {
    try {
      return actualizationApisHolder.getAmountUpdatesLegacyApi(accessToken, serviceUrl)
          .attualizzazione(pagamento);
    } catch (RestClientException e) {
      throw resolveException(e);
    }
  }

  private ActualizationException resolveException(RestClientException e) {
    if (e instanceof RestClientResponseException ex) {
      Error error = ex.getResponseBodyAs(Error.class);
      if (error != null) {
        return new ActualizationException(PuSilErrorDTO.CodeEnum.valueOf(error.getCode().getValue()), error.getMessage());
      }
    }
    return new ActualizationException(PuSilErrorDTO.CodeEnum.GENERIC_ERROR, e.getMessage());
  }
}
