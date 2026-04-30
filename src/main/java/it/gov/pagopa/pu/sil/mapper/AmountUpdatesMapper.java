package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import org.springframework.stereotype.Component;

@Component
public class AmountUpdatesMapper {

  private final BalanceMapper balanceMapper;

  public AmountUpdatesMapper(BalanceMapper balanceMapper) {
    this.balanceMapper = balanceMapper;
  }

  public ActualizationResultDTO pagamentoAggiornato2AmountUpdatesDTO(PagamentoAggiornato pagamentoAggiornato) {
    return ActualizationResultDTO.builder()
      .nav(pagamentoAggiornato.getNumeroAvviso())
      .iun(pagamentoAggiornato.getIun())
      .notificationFeeCents(pagamentoAggiornato.getSpeseNotifica())
      .displayDate(pagamentoAggiornato.getDataVisualizzazione())
      .updatedAmountCents(pagamentoAggiornato.getImportoPosizione())
      .completionDeadlineDate(pagamentoAggiornato.getDataPerfezionamentoDecorrenzaTermini())
      .balance(balanceMapper.mapBalanceFromSil(pagamentoAggiornato.getBilancio()))
      .build();
  }

  public ActualizationResultDTO updatedPayment2AmountUpdatesDTO(UpdatedPayment updatedPayment) {
    return ActualizationResultDTO.builder()
      .nav(updatedPayment.getNav())
      .iun(updatedPayment.getIun())
      .notificationFeeCents(updatedPayment.getNotificationFeeCents())
      .updatedAmountCents(updatedPayment.getAmountCents())
      .completionDeadlineDate(updatedPayment.getRetentionDate())
      .balance(balanceMapper.mapBalanceFromSil(updatedPayment.getBalance()))
      .build();
  }
}
