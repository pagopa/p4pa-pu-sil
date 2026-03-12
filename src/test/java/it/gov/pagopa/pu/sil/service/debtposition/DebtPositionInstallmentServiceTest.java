package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.querypayments.PaymentStatusRequest;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPositionInstallmentServiceTest {
  @Mock
  private DebtPositionService debtPositionService;
  @Mock
  private SessionIdMapper sessionIdMapper;
  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;

  @InjectMocks
  private DebtPositionInstallmentService installmentService;

  private String accessToken;
  private Organization org;
  private DebtPositionDTO dp;
  private DebtPositionDTO otherDp;
  private InstallmentDTO inst;
  private List<Long> installmentIds = new ArrayList<>();
  private List<Pair<DebtPositionDTO, InstallmentDTO>> pairList = new ArrayList<>();

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void init() {
    accessToken = "accessToken";
    org = podamFactory.manufacturePojo(Organization.class);
    org.setStatus(OrganizationStatus.ACTIVE);
    installmentIds.add(1L);
    dp = podamFactory.manufacturePojo(DebtPositionDTO.class);
    dp.setOrganizationId(org.getOrganizationId());
    dp.setStatus(DebtPositionStatus.PAID);
    inst = dp.getPaymentOptions().getFirst().getInstallments().getFirst();
    inst.setInstallmentId(1L);
    inst.setStatus(InstallmentStatus.PAID);
    pairList.add(Pair.of(dp, inst));
    otherDp = podamFactory.manufacturePojo(DebtPositionDTO.class);
    otherDp.setOrganizationId(org.getOrganizationId());
    otherDp.setStatus(DebtPositionStatus.PAID);
  }

  @AfterEach
  void tearDown() {
    installmentIds.clear();
    pairList.clear();
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
  void whenGetDebtPositionsAndInstallmentsByMultipleInstallmentIdsThenSuccess() {
    // Arrange
    String sessionId = "1-2-3";
    installmentIds.add(2L);
    installmentIds.add(3L);
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), INSTALLMENT_ID, sessionId, false);

    InstallmentDTO inst2 = dp.getPaymentOptions().getFirst().getInstallments().getLast();
    inst2.setInstallmentId(2L);
    pairList.add(Pair.of(dp, inst2));

    DebtPositionDTO dp3 = podamFactory.manufacturePojo(DebtPositionDTO.class);
    dp3.setOrganizationId(org.getOrganizationId());
    dp3.setStatus(DebtPositionStatus.PAID);
    InstallmentDTO inst3 = dp3.getPaymentOptions().getFirst().getInstallments().getFirst();
    inst3.setInstallmentId(3L);
    pairList.add(Pair.of(dp3, inst3));

    when(sessionIdMapper.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(1L, accessToken)).thenReturn(dp);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(2L, accessToken)).thenReturn(dp);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(3L, accessToken)).thenReturn(dp3);

    // Act
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = installmentService.getDebtPositionsAndInstallmentsByInstallmentId(
      request, accessToken);

    // Assert
    assertEquals(3, result.size());
    IntStream.range(0, result.size())
      .forEach(i -> assertEquals(pairList.get(i), result.get(i)));
  }

  @Test
  void whenGetDebtPositionsAndInstallmentsByMultipleInstallmentIdsWithMismatchThenSilFaultException() {
    // Arrange
    String sessionId = "1-2-3";
    installmentIds.add(2L);
    installmentIds.add(3L);
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), INSTALLMENT_ID, sessionId, false);

    DebtPositionDTO dp2 = podamFactory.manufacturePojo(DebtPositionDTO.class);
    dp2.setOrganizationId(org.getOrganizationId());
    dp2.setStatus(DebtPositionStatus.PAID);
    InstallmentDTO inst2 = dp2.getPaymentOptions().getFirst().getInstallments().getFirst();
    inst2.setInstallmentId(999L);

    when(sessionIdMapper.mapSessionIdToInstallmentIds(sessionId)).thenReturn(installmentIds);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(1L, accessToken)).thenReturn(dp);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(2L, accessToken)).thenReturn(dp2);

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
      .getDebtPositionsByOrganizationIdAndIud(org.getOrganizationId(), inst.getIud(), Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken))
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
      .getDebtPositionsByOrganizationIdAndIud(org.getOrganizationId(), inst.getIud(), Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken))
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

    when(debtPositionService.getDebtPositionsByOrganizationIdAndIuv(org.getOrganizationId(), inst.getIuv(), Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken)).thenReturn(List.of(dp));

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

    when(debtPositionService.getDebtPositionsByOrganizationIdAndIuv(org.getOrganizationId(), inst.getIuv(), Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken))
      .thenReturn(List.of(otherDp));

    // Act & Assert
    assertThrows(SilFaultException.class, () ->
      installmentService.getDebtPositionsAndInstallmentsByIuv(
        request, org, accessToken)
    );
  }

  @Test
  void givenDebtPositionCancelledWhenGetDebtPositionsAndInstallmentsByIuvThenReturnEmptyList() {
    // Arrange
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), NOTICE_NUMBER, inst.getIuv(), false);
    dp.setStatus(DebtPositionStatus.CANCELLED);

    when(debtPositionService.getDebtPositionsByOrganizationIdAndIuv(org.getOrganizationId(), inst.getIuv(), Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken))
      .thenReturn(List.of(dp));

    // Act
    List<Pair<DebtPositionDTO, InstallmentDTO>> result = installmentService.getDebtPositionsAndInstallmentsByIuv(
      request, org, accessToken);
    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetCategoryWhenLegacyIsValid() {
    String legacyPaymentMetadata = "9/1646246AP/long/long";
    String debtPositionTypeOrgCode = "CODE";
    Long orgId = 1L;

    String result = installmentService.getCategory(legacyPaymentMetadata, debtPositionTypeOrgCode, orgId, accessToken);

    assertEquals("1646246AP", result);
  }

  @Test
  void testGetCategoryWhenLegacyIsNotValid() {
    String legacyPaymentMetadata = "9/1646246AX";
    String debtPositionTypeOrgCode = "CODE";
    Long orgId = 1L;

    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionType debtPositionType = podamFactory.manufacturePojo(DebtPositionType.class);
    debtPositionType.setTaxonomyCode("9/1122333AP/");

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(orgId, debtPositionTypeOrgCode, accessToken))
      .thenReturn(debtPositionTypeOrg);
    when(debtPositionTypeServiceMock.getDebtPositionTypeById(debtPositionTypeOrg.getDebtPositionTypeId(), accessToken))
      .thenReturn(debtPositionType);

    String result = installmentService.getCategory(legacyPaymentMetadata, debtPositionTypeOrgCode, orgId, accessToken);

    assertEquals("1122333AP", result);
  }

  @Test
  void testGetCategoryWhenTypeCodeNotValid() {
    String legacyPaymentMetadata = "9/1646246AX";
    String debtPositionTypeOrgCode = "CODE";
    Long orgId = 1L;

    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(orgId, debtPositionTypeOrgCode, accessToken))
      .thenReturn(null);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () ->  installmentService.getCategory(legacyPaymentMetadata, debtPositionTypeOrgCode, orgId, accessToken));

    assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, exception.getFault());
    assertTrue(exception.getDescription().contains("Tipo dovuto non valido: " + debtPositionTypeOrgCode));
  }

  @Test
  void givenNullDpWhenGetDebtPositionsAndInstallmentsByInstallmentIdThenThrowSilFaultException() {
    PaymentStatusRequest request = new PaymentStatusRequest(org.getIpaCode(), INSTALLMENT_ID, inst.getIuv(), false);

    when(sessionIdMapper.mapSessionIdToInstallmentIds(request.id())).thenReturn(installmentIds);
    when(debtPositionService.getDebtPositionDTOByInstallmentId(inst.getInstallmentId(), accessToken)).thenReturn(null);

    SilFaultException exception = assertThrows(SilFaultException.class, () ->
      installmentService.getDebtPositionsAndInstallmentsByInstallmentId(request, accessToken)
    );

    assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO, exception.getFault());
    assertEquals("id session non valido", exception.getDescription());
  }
}
