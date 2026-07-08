package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMapperTest {
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;

  @Mock
  private TransferMapper transferMapperMock;

  @InjectMocks
  private PaymentMapper mapper;

  @Test
  void testMapToPaymentDTO() {
    // Given
    String accessToken = "test-access-token";
    Long installmentId = 1001L;
    String iud = "IUD-123456";
    Long amountCents = 10000L;
    String remittanceInfo = "Payment for services";
    String balance = "5000";
    PersonDTO debtorInfo =
      podamFactory.manufacturePojo(PersonDTO.class);

    TransferDTO debtPositionTransfer = TransferDTO.builder()
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

    InstallmentDTO installment = InstallmentDTO.builder()
      .installmentId(installmentId)
      .iud(iud)
      .amountCents(amountCents)
      .remittanceInformation(remittanceInfo)
      .notificationFeeCents(100L)
      .balance(balance)
      .debtor(debtorInfo)
      .transfers(List.of(debtPositionTransfer))
      .build();

    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);

    it.gov.pagopa.pu.sil.dto.generated.TransferDTO silTransfer =
      it.gov.pagopa.pu.sil.dto.generated.TransferDTO.builder()
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

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByInstallmentId(installmentId, accessToken))
      .thenReturn(debtPositionTypeOrg);
    when(transferMapperMock.mapToSilTransferDTO(debtPositionTransfer, installment.getOriginalRemittanceInformation()))
      .thenReturn(silTransfer);

    // When
    PaymentDTO result = mapper.mapToPaymentDTO(installment, accessToken);

    // Then
    assertNotNull(result);
    assertEquals(debtPositionTypeOrg.getCode(), result.getDebtPositionTypeOrgCode());
    assertEquals(iud, result.getIud());
    assertEquals(amountCents, result.getTotalAmountCents());
    assertEquals(remittanceInfo, result.getDescription());
    assertEquals(balance, result.getBalance());
    assertNotNull(result.getDebtor());
    assertEquals(debtorInfo.getFiscalCode(), result.getDebtor().getFiscalCode());
    assertEquals(debtorInfo.getFullName(), result.getDebtor().getFullName());
    assertEquals(100L, result.getNotificationFeeCents());
    assertNotNull(result.getTransfers());
    assertEquals(1, result.getTransfers().size());
    assertEquals(silTransfer, result.getTransfers().getFirst());

    TestUtils.checkNotNullFields(result, "stationId");
    result.getTransfers().forEach(TestUtils::checkNotNullFields);
  }
}
