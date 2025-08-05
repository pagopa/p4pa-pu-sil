package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptTransferDTO;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptWithAdditionalNodeDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ReceiptMapper {
  private final ReceiptService receiptService;

  public ReceiptMapper(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  public ReceiptWithAdditionalNodeDataDTO map2ReceiptWithAdditionalNodeDataDTO(InstallmentDTO installment,
                                                                               String accessToken) {
    log.debug("fetch Receipt by Id {} to map to ReceiptWithAdditionalNodeDataDTO", installment.getReceiptId());
    ReceiptDTO receipt = receiptService.getReceiptById(installment.getReceiptId(), accessToken);

    return ReceiptWithAdditionalNodeDataDTO.builder()
      .paymentReceiptId(receipt.getPaymentReceiptId())
      .noticeNumber(receipt.getNoticeNumber())
      .paymentNote(receipt.getPaymentNote())
      .orgFiscalCode(receipt.getOrgFiscalCode())
      .outcome(receipt.getOutcome())
      .creditorReferenceId(receipt.getCreditorReferenceId())
      .paymentAmountCents(receipt.getPaymentAmountCents())
      .description(receipt.getDescription())
      .companyName(receipt.getCompanyName())
      .officeName(receipt.getOfficeName())
      .idPsp(receipt.getIdPsp())
      .pspFiscalCode(receipt.getPspFiscalCode())
      .pspPartitaIva(receipt.getPspPartitaIva())
      .pspCompanyName(receipt.getPspCompanyName())
      .idChannel(receipt.getIdChannel())
      .channelDescription(receipt.getChannelDescription())
      .paymentMethod(receipt.getPaymentMethod())
      .feeCents(receipt.getFeeCents())
      .paymentDateTime(receipt.getPaymentDateTime())
      .applicationDate(receipt.getApplicationDate())
      .transferDate(receipt.getTransferDate())
      .standin(receipt.getStandin())
      .debtor(receipt.getDebtor())
      .payer(receipt.getPayer())
      .transfers(installment.getTransfers().stream().map(this::map).toList())
      .build();
  }

  private ReceiptTransferDTO map(TransferDTO transfer) {
    return ReceiptTransferDTO.builder()
      .transferIndex(transfer.getTransferIndex())
      .amountCents(transfer.getAmountCents())
      .orgFiscalCode(transfer.getOrgFiscalCode())
      .orgName(transfer.getOrgName())
      .mbdAttachment(transfer.getMbdAttachment().getBytes(StandardCharsets.UTF_8))
      .iban(transfer.getIban())
      .remittanceInformation(transfer.getRemittanceInformation())
      .category(transfer.getCategory())
      .build();
  }
}
