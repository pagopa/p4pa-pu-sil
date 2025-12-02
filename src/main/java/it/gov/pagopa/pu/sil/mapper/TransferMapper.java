package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import org.springframework.stereotype.Service;

@Service
public class TransferMapper {

  public TransferDTO mapToDebtPositionTransferDTO(
      it.gov.pagopa.pu.sil.dto.generated.TransferDTO silTransferDTO) {
    return TransferDTO.builder()
      .amountCents(silTransferDTO.getAmountCents())
      .category(silTransferDTO.getCategory())
      .orgFiscalCode(silTransferDTO.getOrgFiscalCode())
      .orgName(silTransferDTO.getOrgName())
      .remittanceInformation(silTransferDTO.getRemittanceInformation())
      .transferIndex(silTransferDTO.getTransferIndex())
      .stampHashDocument(silTransferDTO.getStampHashDocument())
      .stampType(silTransferDTO.getStampType())
      .stampProvincialResidence(silTransferDTO.getStampProvincialResidence())
      .iban(silTransferDTO.getIban())
      .build();
  }

  public it.gov.pagopa.pu.sil.dto.generated.TransferDTO mapToSilTransferDTO(
      TransferDTO debtPositionTransferDTO) {
    return it.gov.pagopa.pu.sil.dto.generated.TransferDTO.builder()
      .amountCents(debtPositionTransferDTO.getAmountCents())
      .category(debtPositionTransferDTO.getCategory())
      .orgFiscalCode(debtPositionTransferDTO.getOrgFiscalCode())
      .orgName(debtPositionTransferDTO.getOrgName())
      .remittanceInformation(debtPositionTransferDTO.getRemittanceInformation())
      .transferIndex(debtPositionTransferDTO.getTransferIndex())
      .stampHashDocument(debtPositionTransferDTO.getStampHashDocument())
      .stampType(debtPositionTransferDTO.getStampType())
      .stampProvincialResidence(debtPositionTransferDTO.getStampProvincialResidence())
      .iban(debtPositionTransferDTO.getIban())
      .postalIban(debtPositionTransferDTO.getPostalIban())
      .build();
  }
}
