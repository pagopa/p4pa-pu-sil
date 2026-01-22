package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Test;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransferMapperTest {
  private final TransferMapper transferMapper = new TransferMapper();

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void mapToDebtPositionTransferDTO() {
    // Given
    it.gov.pagopa.pu.sil.dto.generated.TransferDTO silTransferDTO = it.gov.pagopa.pu.sil.dto.generated.TransferDTO.builder()
      .amountCents(1000L)
      .category("Utilities")
      .orgFiscalCode("12345678901")
      .orgName("Test Organization")
      .remittanceInformation("Payment for services")
      .transferIndex(1)
      .stampHashDocument("hash123")
      .stampType("TypeA")
      .stampProvincialResidence("ProvinceX")
      .iban("IT60X0542811101000000123456")
      .build();

    // When
    TransferDTO result = transferMapper.mapToDebtPositionTransferDTO(silTransferDTO);

    // Then
    assertNotNull(result);
    assertEquals(silTransferDTO.getAmountCents(), result.getAmountCents());
    assertEquals(silTransferDTO.getCategory(), result.getCategory());
    assertEquals(silTransferDTO.getOrgFiscalCode(), result.getOrgFiscalCode());
    assertEquals(silTransferDTO.getOrgName(), result.getOrgName());
    assertEquals(silTransferDTO.getRemittanceInformation(), result.getRemittanceInformation());
    assertEquals(silTransferDTO.getTransferIndex(), result.getTransferIndex());
    assertEquals(silTransferDTO.getStampHashDocument(), result.getStampHashDocument());
    assertEquals(silTransferDTO.getStampType(), result.getStampType());
    assertEquals(silTransferDTO.getStampProvincialResidence(), result.getStampProvincialResidence());
    assertEquals(silTransferDTO.getIban(), result.getIban());

    TestUtils.checkNotNullFields(result, "transferId", "installmentId", "postalIban",
      "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "mbdAttachment");
  }

  @Test
  void mapToSilTransferDTO() {
    // Given
    TransferDTO debtPositionTransferDTO = TransferDTO.builder()
      .amountCents(1000L)
      .category("Utilities")
      .orgFiscalCode("12345678901")
      .orgName("Test Organization")
      .remittanceInformation("Payment for services")
      .transferIndex(1)
      .stampHashDocument("hash123")
      .stampType("TypeA")
      .stampProvincialResidence("ProvinceX")
      .iban("IT60X0542811101000000123456")
      .postalIban("IT60X0542811101000000654321")
      .build();
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.transfers(List.of(debtPositionTransferDTO));

    // When
    List<it.gov.pagopa.pu.sil.dto.generated.TransferDTO> result = transferMapper.mapToSilTransferDTO(installmentDTO);

    // Then
    assertNotNull(result);
    it.gov.pagopa.pu.sil.dto.generated.TransferDTO first = result.getFirst();
    assertEquals(debtPositionTransferDTO.getAmountCents(), first.getAmountCents());
    assertEquals(debtPositionTransferDTO.getCategory(), first.getCategory());
    assertEquals(debtPositionTransferDTO.getOrgFiscalCode(), first.getOrgFiscalCode());
    assertEquals(debtPositionTransferDTO.getOrgName(), first.getOrgName());
    assertEquals(debtPositionTransferDTO.getRemittanceInformation(), first.getRemittanceInformation());
    assertEquals(debtPositionTransferDTO.getTransferIndex(), first.getTransferIndex());
    assertEquals(debtPositionTransferDTO.getStampHashDocument(), first.getStampHashDocument());
    assertEquals(debtPositionTransferDTO.getStampType(), first.getStampType());
    assertEquals(debtPositionTransferDTO.getStampProvincialResidence(), first.getStampProvincialResidence());
    assertEquals(debtPositionTransferDTO.getIban(), first.getIban());
    assertEquals(debtPositionTransferDTO.getPostalIban(), first.getPostalIban());

    TestUtils.checkNotNullFields(first);
  }
}
