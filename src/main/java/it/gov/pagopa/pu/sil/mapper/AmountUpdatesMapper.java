package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import org.springframework.stereotype.Component;

@Component
public class AmountUpdatesMapper {
  public AmountUpdatesDTO pagamentoAggiornato2AmountUpdatesDTO(PagamentoAggiornato pagamentoAggiornato) {
    if (pagamentoAggiornato == null) {
      return AmountUpdatesDTO.builder()
        .outcome(AmountUpdatesDTO.OutcomeEnum.KO)
        .isBlockingError(false)
        .build();
    }
    return AmountUpdatesDTO.builder()
      .outcome(AmountUpdatesDTO.OutcomeEnum.OK)
      .nav(pagamentoAggiornato.getNumeroAvviso())
      .iun(pagamentoAggiornato.getIun())
      .notificationFee(pagamentoAggiornato.getSpeseNotifica())
      .displayDate(pagamentoAggiornato.getDataVisualizzazione())
      .updatedAmount(pagamentoAggiornato.getImportoPosizione())
      .completionDeadlineDate(pagamentoAggiornato.getDataPerfezionamentoDecorrenzaTermini())
      .balance(pagamentoAggiornato.getBilancio())
      .errorCode(pagamentoAggiornato.getCodice().getValue())
      .errorDescription(pagamentoAggiornato.getDettaglio())
      .build();
  }
}
