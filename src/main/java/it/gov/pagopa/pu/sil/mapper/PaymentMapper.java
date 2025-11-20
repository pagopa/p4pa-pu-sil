package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentDTO;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
  private final DebtPositionTypeService debtPositionTypeService;
  private final TransferMapper transferMapper;

  public PaymentMapper(DebtPositionTypeService debtPositionTypeService, TransferMapper transferMapper) {
    this.debtPositionTypeService = debtPositionTypeService;
    this.transferMapper = transferMapper;
  }

  public PaymentDTO mapToPaymentDTO(InstallmentDTO installment, String accessToken) {
    DebtPositionTypeOrg debtPositionTypeOrg = debtPositionTypeService.getDebtPositionTypeOrgByInstallmentId(installment.getInstallmentId(), accessToken);

    return PaymentDTO.builder()
      .debtPositionTypeOrgCode(debtPositionTypeOrg.getCode())
      .notificationFeeCents(installment.getNotificationFeeCents())
      .iud(installment.getIud())
      .totalAmountCents(installment.getAmountCents())
      .description(installment.getRemittanceInformation())
      .balance(installment.getBalance())
      .debtor(installment.getDebtor())
      .transfers(installment.getTransfers().stream()
        .map(transferMapper::mapToSilTransferDTO)
        .toList())
      .build();
  }
}
