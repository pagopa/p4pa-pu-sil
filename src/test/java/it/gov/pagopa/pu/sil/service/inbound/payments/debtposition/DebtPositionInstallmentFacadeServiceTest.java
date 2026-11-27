package it.gov.pagopa.pu.sil.service.inbound.payments.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.PaymentStatusRequest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPositionInstallmentFacadeServiceTest {
  @Mock
  private DebtPositionInstallmentService debtPositionInstallmentServiceMock;

  private Organization org;

  private DebtPositionInstallmentFacadeService facadeService;

  private final String accessToken = "testAccessToken";
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    facadeService = new DebtPositionInstallmentFacadeService(debtPositionInstallmentServiceMock);
    org = podamFactory.manufacturePojo(Organization.class);
    org.ipaCode("orgIpa");
  }


  @Test
  void whenFetchByInstallmentIdInvokeGetDebtPositionsAndInstallmentsByInstallmentId() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), QueryPaymentStatusType.INSTALLMENT_ID, "12345", false);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installment = podamFactory.manufacturePojo(InstallmentDTO.class);
    List<Pair<DebtPositionDTO, InstallmentDTO>> expectedResult = List.of(
      Pair.of(debtPosition, installment)
    );

    when(debtPositionInstallmentServiceMock.getDebtPositionsAndInstallmentsByInstallmentId(request, accessToken))
        .thenReturn(expectedResult);

    // When
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = facadeService.fetch(request, null, accessToken);

    // Then
    assertEquals(expectedResult, result);
  }

  @Test
  void whenFetchByIudThenInvokeGetDebtPositionsAndInstallmentsByIud() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), QueryPaymentStatusType.IUD, "IUD_12345", false);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installment = podamFactory.manufacturePojo(InstallmentDTO.class);
    List<Pair<DebtPositionDTO, InstallmentDTO>> expectedResult = List.of(
      Pair.of(debtPosition, installment)
    );

    // Mock the service call
    when(debtPositionInstallmentServiceMock.getDebtPositionsAndInstallmentsByIud(request, org, accessToken))
      .thenReturn(expectedResult);

    // When
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = facadeService.fetch(request, org, accessToken);

    // Then
    assertEquals(expectedResult, result);
  }

  @Test
  void whenFetchByNoticeNumberThenInvokeGetDebtPositionsAndInstallmentsByIuv() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), QueryPaymentStatusType.NOTICE_NUMBER, "30123456789", false);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    InstallmentDTO installment = podamFactory.manufacturePojo(InstallmentDTO.class);
    List<Pair<DebtPositionDTO, InstallmentDTO>> expectedResult = List.of(
      Pair.of(debtPosition, installment)
    );

    // Mock the service call
    when(debtPositionInstallmentServiceMock.getDebtPositionsAndInstallmentsByIuv(request, org, accessToken))
      .thenReturn(expectedResult);

    // When
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = facadeService.fetch(request, org, accessToken);

    // Then
    assertEquals(expectedResult, result);
  }
}
