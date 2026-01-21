package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptTransferDTO;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptWithAdditionalNodeDataDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReceiptMapperTest {
  @Mock
  private ReceiptService receiptService;

  @InjectMocks
  private ReceiptMapper receiptMapper;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @ParameterizedTest
  @MethodSource("provideRemittanceInformation")
  void testMap2ReceiptWithAdditionalNodeDataDTO(String remittanceInformation, String originalRemittanceInformation, String expectedRemittanceInformation) {
    // Arrange
    InstallmentDTO installment = podamFactory.manufacturePojo(InstallmentDTO.class);
    installment.setOriginalRemittanceInformation(originalRemittanceInformation);
    TransferDTO transfer = TransferDTO.builder()
      .transferIndex(1)
      .amountCents(1000L)
      .orgFiscalCode("12345678901")
      .orgName("Test Organization")
      .mbdAttachment("Test Attachment")
      .iban("IT60X0542811101000000123456")
      .remittanceInformation(remittanceInformation)
      .category("Utilities")
      .build();
    installment.transfers(List.of(transfer));

    PersonDTO debtor = podamFactory.manufacturePojo(PersonDTO.class);
    PersonDTO payer = podamFactory.manufacturePojo(PersonDTO.class);
    ReceiptDTO receipt = podamFactory.manufacturePojo(ReceiptDTO.class);
    receipt.payer(payer).debtor(debtor);

    Mockito.when(receiptService.getReceiptById(installment.getReceiptId(), "accessToken")).thenReturn(receipt);

    // Act
    ReceiptWithAdditionalNodeDataDTO result = receiptMapper.map2ReceiptWithAdditionalNodeDataDTO(installment, "accessToken");

    // Assert
    receiptWithAdditionalNodeDataDTOAssertions(receipt, result);
    transferDTOAssertions(transfer, result.getTransfers().getFirst(), expectedRemittanceInformation);
  }

  private void receiptWithAdditionalNodeDataDTOAssertions(ReceiptDTO expected, ReceiptWithAdditionalNodeDataDTO actual) {
    Assertions.assertEquals(expected.getPaymentReceiptId(), actual.getPaymentReceiptId());
    Assertions.assertEquals(expected.getNoticeNumber(), actual.getNoticeNumber());
    Assertions.assertEquals(expected.getPaymentNote(), actual.getPaymentNote());
    Assertions.assertEquals(expected.getOrgFiscalCode(), actual.getOrgFiscalCode());
    Assertions.assertEquals(expected.getOutcome(), actual.getOutcome());
    Assertions.assertEquals(expected.getCreditorReferenceId(), actual.getCreditorReferenceId());
    Assertions.assertEquals(expected.getPaymentAmountCents(), actual.getPaymentAmountCents());
    Assertions.assertEquals(expected.getDescription(), actual.getDescription());
    Assertions.assertEquals(expected.getCompanyName(), actual.getCompanyName());
    Assertions.assertEquals(expected.getOfficeName(), actual.getOfficeName());
    Assertions.assertEquals(expected.getIdPsp(), actual.getIdPsp());
    Assertions.assertEquals(expected.getPspFiscalCode(), actual.getPspFiscalCode());
    Assertions.assertEquals(expected.getPspPartitaIva(), actual.getPspPartitaIva());
    Assertions.assertEquals(expected.getPspCompanyName(), actual.getPspCompanyName());
    Assertions.assertEquals(expected.getIdChannel(), actual.getIdChannel());
    Assertions.assertEquals(expected.getChannelDescription(), actual.getChannelDescription());
    Assertions.assertEquals(expected.getPaymentMethod(), actual.getPaymentMethod());
    Assertions.assertEquals(expected.getFeeCents(), actual.getFeeCents());
    Assertions.assertEquals(expected.getPaymentDateTime(), actual.getPaymentDateTime());
    Assertions.assertEquals(expected.getApplicationDate(), actual.getApplicationDate());
    Assertions.assertEquals(expected.getTransferDate(), actual.getTransferDate());
    Assertions.assertEquals(expected.getStandin(), actual.getStandin());
    Assertions.assertEquals(expected.getDebtor(), actual.getDebtor());
    Assertions.assertEquals(expected.getPayer(), actual.getPayer());

    Assertions.assertNotNull(actual.getTransfers());
    Assertions.assertEquals(1, actual.getTransfers().size());

    TestUtils.checkNotNullFields(actual, "metadata");
  }

  private void transferDTOAssertions(TransferDTO expected, ReceiptTransferDTO actual, String expectedRemittanceInformation) {
    Assertions.assertEquals(expected.getTransferIndex(), actual.getTransferIndex());
    Assertions.assertEquals(expected.getAmountCents(), actual.getAmountCents());
    Assertions.assertEquals(expected.getOrgFiscalCode(), actual.getOrgFiscalCode());
    Assertions.assertEquals(expected.getOrgName(), actual.getOrgName());
    Assertions.assertArrayEquals(expected.getMbdAttachment().getBytes(), actual.getMbdAttachment());
    Assertions.assertEquals(expected.getIban(), actual.getIban());
    Assertions.assertEquals(expectedRemittanceInformation, actual.getRemittanceInformation());
    Assertions.assertEquals(expected.getCategory(), actual.getCategory());

    TestUtils.checkNotNullFields(actual, "metadata");
  }

  private static Stream<Arguments> provideRemittanceInformation() {
    return Stream.of(
      Arguments.of("remittanceInformation", null, "remittanceInformation"),
      Arguments.of("remittanceInformation", "originalRemittanceInformation", "remittanceInformation"),
      Arguments.of("REMITTANCE_INFORMATION_PLACEHOLDER with remittanceInformation", "originalRemittanceInformation", "REMITTANCE_INFORMATION_PLACEHOLDER with remittanceInformation")
    );
  }
}
