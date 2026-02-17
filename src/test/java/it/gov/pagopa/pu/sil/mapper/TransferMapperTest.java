package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransferMapperTest {
  private final TransferMapper transferMapper = new TransferMapper();

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

    TestUtils.checkNotNullFields(result, "transferId", "installmentId", "postalIban", "mbdAttachment", "flagOwner",
      "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
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

    // When
    it.gov.pagopa.pu.sil.dto.generated.TransferDTO result = transferMapper.mapToSilTransferDTO(debtPositionTransferDTO, null);

    // Then
    assertNotNull(result);
    assertEquals(debtPositionTransferDTO.getAmountCents(), result.getAmountCents());
    assertEquals(debtPositionTransferDTO.getCategory(), result.getCategory());
    assertEquals(debtPositionTransferDTO.getOrgFiscalCode(), result.getOrgFiscalCode());
    assertEquals(debtPositionTransferDTO.getOrgName(), result.getOrgName());
    assertEquals(debtPositionTransferDTO.getRemittanceInformation(), result.getRemittanceInformation());
    assertEquals(debtPositionTransferDTO.getTransferIndex(), result.getTransferIndex());
    assertEquals(debtPositionTransferDTO.getStampHashDocument(), result.getStampHashDocument());
    assertEquals(debtPositionTransferDTO.getStampType(), result.getStampType());
    assertEquals(debtPositionTransferDTO.getStampProvincialResidence(), result.getStampProvincialResidence());
    assertEquals(debtPositionTransferDTO.getIban(), result.getIban());
    assertEquals(debtPositionTransferDTO.getPostalIban(), result.getPostalIban());

    TestUtils.checkNotNullFields(result);
  }
}
