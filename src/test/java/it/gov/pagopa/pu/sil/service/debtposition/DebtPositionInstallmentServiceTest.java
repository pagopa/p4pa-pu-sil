package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.querypayments.PaymentStatusRequest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType.*;
import static it.gov.pagopa.pu.sil.service.debtposition.InstallmentFacadeService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtPositionInstallmentServiceTest {
  @Mock
  private DebtPositionService debtPositionService;
  @Mock
  private SessionIdMapper sessionIdMapper;

  @InjectMocks
  private DebtPositionInstallmentService installmentService;

  private String accessToken;
  private Organization org;
  private DebtPositionDTO dp;
  private DebtPositionDTO otherDp;
  private InstallmentDTO inst;
  private List<Long> installmentIds;
  private List<Pair<DebtPositionDTO, InstallmentDTO>> pairList;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void init() {
    accessToken = "accessToken";
    org = podamFactory.manufacturePojo(Organization.class);
    org.setStatus(OrganizationStatus.ACTIVE);
    installmentIds = List.of(1L);
    dp = podamFactory.manufacturePojo(DebtPositionDTO.class);
    dp.setOrganizationId(org.getOrganizationId());
    dp.setStatus(DebtPositionStatus.PAID);
    inst = dp.getPaymentOptions().getFirst().getInstallments().getFirst();
    inst.setInstallmentId(1L);
    inst.setStatus(InstallmentStatus.PAID);
    pairList = List.of(Pair.of(dp, inst));
    otherDp = podamFactory.manufacturePojo(DebtPositionDTO.class);
    otherDp.setOrganizationId(org.getOrganizationId());
    otherDp.setStatus(DebtPositionStatus.PAID);
  }

  @Test
  void whenGetDebtPositionsAndInstallmentsByInstallmentIdThenSuccess() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), INSTALLMENT_ID, inst.getInstallmentId().toString(), false);

    when(sessionIdMapper.mapSessionIdToInstallmentIds(inst.getInstallmentId().toString())).thenReturn(installmentIds);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(inst.getInstallmentId(), accessToken)).thenReturn(dp);

    // Act
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = installmentService.getDebtPositionsAndInstallmentsByInstallmentId(
      request, accessToken);
    // Assert
    assertEquals(pairList, result);
  }

  @Test
  void whenGetDebtPositionsAndInstallmentsByInstallmentIdThenSilFaultException() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), INSTALLMENT_ID, inst.getInstallmentId().toString(), false);

    when(sessionIdMapper.mapSessionIdToInstallmentIds(request.id())).thenReturn(installmentIds);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(Long.valueOf(request.id()), accessToken)).thenReturn(otherDp);

    // Act & Assert
    assertThrows(SilFaultException.class, () ->
      installmentService.getDebtPositionsAndInstallmentsByInstallmentId(request, accessToken)
    );
  }

  @Test
  void whenGetDebtPositionsAndInstallmentsByIudThenSuccess() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), IUD, inst.getIud(), false);

    when(debtPositionService
      .getDebtPositionsByOrganizationIdAndIud(org.getOrganizationId(), inst.getIud(), ALLOWED_ORIGINS, accessToken))
      .thenReturn(List.of(dp));
    // Act
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = installmentService.getDebtPositionsAndInstallmentsByIud(
      request, org, accessToken);
    // Assert
    assertEquals(pairList, result);
  }

  @Test
  void whenGetDebtPositionsAndInstallmentsByIudThenSilFaultException() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), IUD, inst.getIud(), false);

    when(debtPositionService
      .getDebtPositionsByOrganizationIdAndIud(org.getOrganizationId(), inst.getIud(), ALLOWED_ORIGINS, accessToken))
      .thenReturn(List.of(otherDp));

    // Act & Assert
    assertThrows(SilFaultException.class, () ->
      installmentService.getDebtPositionsAndInstallmentsByIud(
        request, org, accessToken
      )
    );
  }

  @Test
  void whenGetDebtPositionsAndInstallmentsByIuvThenSuccess() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), NOTICE_NUMBER, inst.getIuv(), false);

    when(debtPositionService.getDebtPositionsByOrganizationIdAndIuv(org.getOrganizationId(), inst.getIuv(), ALLOWED_ORIGINS, accessToken)).thenReturn(List.of(dp));

    // Act
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = installmentService.getDebtPositionsAndInstallmentsByIuv(
      request, org, accessToken);
    // Assert
    assertEquals(pairList, result);
  }

  @Test
  void whenGetDebtPositionsAndInstallmentsByIuvThenSilFaultException() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), NOTICE_NUMBER, inst.getIuv(), false);

    when(debtPositionService.getDebtPositionsByOrganizationIdAndIuv(org.getOrganizationId(), inst.getIuv(), ALLOWED_ORIGINS, accessToken))
      .thenReturn(List.of(otherDp));

    // Act & Assert
    assertThrows(SilFaultException.class, () ->
      installmentService.getDebtPositionsAndInstallmentsByIuv(
        request, org, accessToken)
    );
  }
}
