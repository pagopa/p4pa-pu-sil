package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.sil.paymentnotification.dto.generated.PaymentDataDTO;
import it.gov.pagopa.sil.paymentnotification.dto.generated.PersonDTO;
import it.gov.pagopa.sil.paymentnotification.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentNotificationMapper {

  private final ReceiptService receiptService;

  public PaymentDataDTO mapPaymentData(InstallmentDTO installmentDTO, String orgFiscalCode, String debtPositionTypeOrgCode, String accessToken) {
    ReceiptDTO receipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), accessToken);
    return PaymentDataDTO.builder()
      .orgFiscalCode(orgFiscalCode)
      .debtPositionTypeOrgCode(debtPositionTypeOrgCode)
      .iud(installmentDTO.getIud())
      .iuv(installmentDTO.getIuv())
      .nav(installmentDTO.getNav())
      .amountCents(installmentDTO.getAmountCents())
      .notificationFeeCents(installmentDTO.getNotificationFeeCents())
      .remittanceInformation(Optional.ofNullable(installmentDTO.getOriginalRemittanceInformation())
        .orElse(installmentDTO.getRemittanceInformation()))
      .debtor(mapPerson(installmentDTO.getDebtor()))
      .paymentDate(receipt.getPaymentDateTime())
      .build();
  }

  private PersonDTO mapPerson(it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO dpPerson) {
    return PersonDTO.builder()
      .fiscalCode(dpPerson.getFiscalCode())
      .entityType(PersonEntityType.valueOf(dpPerson.getEntityType().getValue()))
      .fullName(dpPerson.getFullName())
      .email(dpPerson.getEmail())
      .nation(dpPerson.getNation())
      .province(dpPerson.getProvince())
      .location(dpPerson.getLocation())
      .address(dpPerson.getAddress())
      .civic(dpPerson.getCivic())
      .postalCode(dpPerson.getPostalCode())
      .build();
  }
}
