package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPositionMapperTest {
  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;
  @Mock
  private TransferMapper transferMapperMock;

  @InjectMocks
  private DebtPositionMapper mapper;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void testMapper() {
    // Given
    String accessToken = "accessToken";
    Organization organization = podamFactory.manufacturePojo(Organization.class);
    it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO debtPositionSource = podamFactory.manufacturePojo(it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO.class);
    it.gov.pagopa.pu.sil.dto.generated.PaymentOptionDTO paymentOptionSource = podamFactory.manufacturePojo(it.gov.pagopa.pu.sil.dto.generated.PaymentOptionDTO.class);
    it.gov.pagopa.pu.sil.dto.generated.InstallmentDTO installmentSource = podamFactory.manufacturePojo(it.gov.pagopa.pu.sil.dto.generated.InstallmentDTO.class);
    it.gov.pagopa.pu.sil.dto.generated.TransferDTO transferSource = podamFactory.manufacturePojo(it.gov.pagopa.pu.sil.dto.generated.TransferDTO.class);
    TransferDTO transferDTO = podamFactory.manufacturePojo(TransferDTO.class);
    installmentSource.setTransfers(Collections.singletonList(transferSource));
    paymentOptionSource.setInstallments(Collections.singletonList(installmentSource));
    debtPositionSource.setPaymentOptions(Collections.singletonList(paymentOptionSource));

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      organization.getOrganizationId(), debtPositionSource.getDebtPositionTypeOrgCode(), accessToken))
      .thenReturn(podamFactory.manufacturePojo(DebtPositionTypeOrg.class));
    when(transferMapperMock.mapToDebtPositionTransferDTO(transferSource))
      .thenReturn(transferDTO);

    // When
    DebtPositionDTO result = mapper.mapRequestToDebtPosition(debtPositionSource, organization, accessToken);

    // Then
    assertNotNull(result);
    TestUtils.checkAllNotNullFields(result,
      "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
    result.getPaymentOptions().forEach(po -> {
      TestUtils.checkAllNotNullFields(po, "paymentOptionId", "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
      po.getInstallments().forEach(i -> {
        TestUtils.checkAllNotNullFields(i, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa",
          "iur", "iuf", "iun", "notificationFeeCents", "notificationDate", "ingestionFlowFileId",
          "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId", "switchToExpired",
          "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
        i.getTransfers().forEach(t -> {
          TestUtils.checkAllNotNullFields(t, "transferId", "installmentId",
            "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
        });
      });
    });
  }
}
