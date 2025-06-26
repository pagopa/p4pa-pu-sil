package it.gov.pagopa.pu.sil.connector.actualization.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.actualization.legacy.dto.generated.Error;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.exception.ActualizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

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
      throw mapToActualizationException(e);
    }
  }

  private ActualizationException mapToActualizationException(RestClientException e) {
    if (e instanceof HttpStatusCodeException ex) {
      String responseBody = ex.getResponseBodyAsString();
      try {
        ObjectMapper mapper = new ObjectMapper();
        Error error = mapper.readValue(responseBody, Error.class);
        return new ActualizationException(error.getCode().getValue(), error.getMessage());
      } catch (Exception ignored) {
        // Ignore deserialization errors
      }
      return new ActualizationException("GENERIC_ERROR", String.format("HTTP %d: %s", ex.getStatusCode().value(), ex.getStatusText()));
    }
    return new ActualizationException("GENERIC_ERROR", e.getMessage());
  }
}
