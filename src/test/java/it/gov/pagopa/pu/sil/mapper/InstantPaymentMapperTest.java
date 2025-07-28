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

    PaymentOptionDTO po = dp.getPaymentOptions().getFirst();
    assertEquals(PaymentOptionStatus.UNPAID, po.getStatus());
    assertEquals(1, po.getPaymentOptionIndex());
    assertEquals(PaymentOptionTypeEnum.SINGLE_INSTALLMENT, po.getPaymentOptionType());
    assertEquals(paymentDTO.getTotalAmountCents(), po.getTotalAmountCents());
    assertEquals(transferDTO.getRemittanceInformation(), po.getDescription());

    InstallmentDTO i = po.getInstallments().getFirst();
    assertEquals(InstallmentStatus.UNPAID, i.getStatus());
    assertEquals(paymentDTO.getIud(), i.getIud());
    assertEquals(transferDTO.getAmountCents(), i.getAmountCents());
    assertEquals(paymentDTO.getDebtor(), i.getDebtor());
    assertEquals(transferDTO.getCategory(), i.getLegacyPaymentMetadata());
    assertEquals(transferDTO.getRemittanceInformation(), i.getRemittanceInformation());
    assertTrue(i.getSourceFlowName().contains("CART_ID"));

    TestUtils.checkNotNullFields(dp,
      "debtPositionId", "validityDate", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
    dp.getPaymentOptions().forEach(po2 -> {
      TestUtils.checkNotNullFields(po2, "paymentOptionId", "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
      po2.getInstallments().forEach(i2 -> {
        TestUtils.checkNotNullFields(i2, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa",
          "iuv", "iur", "iuf", "nav", "iun", "notificationFeeCents", "notificationDate", "ingestionFlowFileId",
          "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId", "switchToExpired",
          "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
        if(i2.getTransfers()!=null) {
          i2.getTransfers().forEach(t -> {
            TestUtils.checkNotNullFields(t, "transferId", "installmentId",
              "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
          });
        }
      });
    });
  }
}
