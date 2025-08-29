package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentDTO;
import it.gov.pagopa.pu.sil.dto.generated.TransferDTO;
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
class InstantPaymentMapperTest {
  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;
  @Mock
  private TransferMapper transferMapperMock;

  @InjectMocks
  private InstantPaymentMapper mapper;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void testMapRequestToDebtPositions() {

    // Given
    InstantPaymentRequest request = podamFactory.manufacturePojo(InstantPaymentRequest.class);
    PaymentDTO paymentDTO = podamFactory.manufacturePojo(PaymentDTO.class);
    TransferDTO transferDTO = podamFactory.manufacturePojo(TransferDTO.class);
    paymentDTO.setTransfers(List.of(transferDTO));
    request.setPayments(List.of(paymentDTO));
    Organization org = podamFactory.manufacturePojo(Organization.class);
    org.setIpaCode("ORG_IPA");
    org.setStatus(OrganizationStatus.ACTIVE);
    String accessToken = "accessToken";

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), paymentDTO.getDebtPositionTypeOrgCode(), accessToken))
      .thenReturn(podamFactory.manufacturePojo(DebtPositionTypeOrg.class));
    when(transferMapperMock.mapToDebtPositionTransferDTO(transferDTO))
      .thenReturn(podamFactory.manufacturePojo(it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO.class));
    // When
    List<DebtPositionDTO> result = mapper.mapRequestToDebtPositions(request, org,"CART_ID", accessToken);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());

    DebtPositionDTO dp = result.getFirst();

    assertEquals(org.getOrganizationId(), dp.getOrganizationId());
    assertEquals(paymentDTO.getDescription(), dp.getDescription());
    assertTrue(dp.getIupdOrg().contains("CART_ID"));
    assertEquals(DebtPositionStatus.UNPAID, dp.getStatus());
    assertEquals(DebtPositionOrigin.SPONTANEOUS_SIL, dp.getDebtPositionOrigin());

    PaymentOptionDTO poDTO = dp.getPaymentOptions().getFirst();
    assertEquals(PaymentOptionStatus.UNPAID, poDTO.getStatus());
    assertEquals(1, poDTO.getPaymentOptionIndex());
    assertEquals(PaymentOptionTypeEnum.SINGLE_INSTALLMENT, poDTO.getPaymentOptionType());
    assertEquals(paymentDTO.getTotalAmountCents(), poDTO.getTotalAmountCents());
    assertEquals(transferDTO.getRemittanceInformation(), poDTO.getDescription());

    InstallmentDTO installmentDTO = poDTO.getInstallments().getFirst();
    assertEquals(InstallmentStatus.UNPAID, installmentDTO.getStatus());
    assertEquals(paymentDTO.getIud(), installmentDTO.getIud());
    assertEquals(paymentDTO.getTotalAmountCents(), installmentDTO.getAmountCents());
    assertEquals(paymentDTO.getDebtor(), installmentDTO.getDebtor());
    assertEquals(transferDTO.getRemittanceInformation(), installmentDTO.getRemittanceInformation());
    assertTrue(installmentDTO.getSourceFlowName().contains("CART_ID"));

    TestUtils.checkNotNullFields(dp,
      "debtPositionId", "validityDate", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
    dp.getPaymentOptions().forEach(po -> {
      TestUtils.checkNotNullFields(po, "paymentOptionId", "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
      po.getInstallments().forEach(i -> {
        TestUtils.checkNotNullFields(i, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa", "generateNotice",
          "iuv", "iur", "iuf", "nav", "iun", "notificationFeeCents", "notificationDate", "ingestionFlowFileId",
          "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId", "switchToExpired",
          "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "legacyPaymentMetadata");
        if(i.getTransfers()!=null) {
          i.getTransfers().forEach(t -> {
            TestUtils.checkNotNullFields(t, "transferId", "installmentId",
              "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
          });
        }
      });
    });
  }
}
