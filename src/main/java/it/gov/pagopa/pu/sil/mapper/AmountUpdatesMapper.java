package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AmountUpdatesMapper {
  public AmountUpdatesDTO pagamentoAggiornato2AmountUpdatesDTO(PagamentoAggiornato pagamentoAggiornato) {
    return AmountUpdatesDTO.builder()
      .outcome(AmountUpdatesDTO.OutcomeEnum.OK)
      .nav(pagamentoAggiornato.getNumeroAvviso())
      .iun(pagamentoAggiornato.getIun())
      .notificationFee(pagamentoAggiornato.getSpeseNotifica())
      .displayDate(pagamentoAggiornato.getDataVisualizzazione())
      .updatedAmount(pagamentoAggiornato.getImportoPosizione())
      .completionDeadlineDate(pagamentoAggiornato.getDataPerfezionamentoDecorrenzaTermini())
      .balance(pagamentoAggiornato.getBilancio())
      .errorCode(Optional.ofNullable(pagamentoAggiornato.getCodice()).map(PagamentoAggiornato.CodiceEnum::getValue).orElse(null))
      .errorDescription(pagamentoAggiornato.getDettaglio())
      .isBlockingError(PagamentoAggiornato.CodiceEnum._004.getValue().equals(
        Optional.ofNullable(pagamentoAggiornato.getCodice())
          .map(PagamentoAggiornato.CodiceEnum::getValue)
          .orElse(null)
        )
      ).build();
  }

  public AmountUpdatesDTO mapToKoAmountUpdatesDTO() {
    return AmountUpdatesDTO.builder()
      .outcome(AmountUpdatesDTO.OutcomeEnum.KO)
      .isBlockingError(false)
      .build();
  }
}
