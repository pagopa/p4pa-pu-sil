package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato.CodiceEnum;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.exception.PaymentInvalidStatusException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.exception.PaymentNotNotifiedException;
import it.gov.pagopa.pu.sil.mapper.AmountUpdatesMapper;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import org.springframework.stereotype.Component;


@Component
public class LegacyActualizationServiceImpl implements LegacyActualizationService {
  private final LegacyActualizationClient legacyActualizationClient;
  private final AmountUpdatesMapper amountUpdatesMapper;

  public LegacyActualizationServiceImpl(LegacyActualizationClient legacyActualizationClient,
                                        AmountUpdatesMapper amountUpdatesMapper) {
    this.legacyActualizationClient = legacyActualizationClient;
    this.amountUpdatesMapper = amountUpdatesMapper;
  }

  @Override
  public ActualizationResultDTO actualization(RegistryContextData contextData, String accessToken, String serviceUrl, Pagamento pagamento) {
      PagamentoAggiornato actualization = legacyActualizationClient.actualization(contextData, accessToken, serviceUrl, pagamento);
      return validateOutcome(actualization);
  }

  private ActualizationResultDTO validateOutcome(PagamentoAggiornato actualization) {
    if (actualization.getCodice() != null) {
      String message = actualization.getDettaglio();
      switch (actualization.getCodice()) {
        case CodiceEnum._002:
          throw new PaymentNotFoundException(message);
        case CodiceEnum._003:
          throw new PaymentNotNotifiedException(message);
        case CodiceEnum._004:
          throw new PaymentInvalidStatusException(message);
      }
    }
    return amountUpdatesMapper.pagamentoAggiornato2AmountUpdatesDTO(actualization);
  }
}
