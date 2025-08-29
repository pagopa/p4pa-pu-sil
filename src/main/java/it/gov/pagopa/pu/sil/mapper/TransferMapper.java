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
}
