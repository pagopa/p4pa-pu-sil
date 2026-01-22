package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.sil.util.Utilities;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

  public List<it.gov.pagopa.pu.sil.dto.generated.TransferDTO> mapToSilTransferDTO(InstallmentDTO installmentDTO) {
    return installmentDTO.getTransfers().stream()
      .map(transferDTO -> it.gov.pagopa.pu.sil.dto.generated.TransferDTO.builder()
          .amountCents(transferDTO.getAmountCents())
          .category(transferDTO.getCategory())
          .orgFiscalCode(transferDTO.getOrgFiscalCode())
          .orgName(transferDTO.getOrgName())
          .remittanceInformation(Utilities.resolveRemittanceInformation(
            transferDTO.getRemittanceInformation(),
            installmentDTO.getOriginalRemittanceInformation()))
          .transferIndex(transferDTO.getTransferIndex())
          .stampHashDocument(transferDTO.getStampHashDocument())
          .stampType(transferDTO.getStampType())
          .stampProvincialResidence(transferDTO.getStampProvincialResidence())
          .iban(transferDTO.getIban())
          .postalIban(transferDTO.getPostalIban())
          .build()
      ).collect(Collectors.toList());
  }
}
