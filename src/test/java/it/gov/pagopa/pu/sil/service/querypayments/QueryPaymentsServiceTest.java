package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentStatusResponseDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptWithAdditionalNodeDataDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.ReceiptMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.debtposition.InstallmentFacadeService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType.PAYMENT_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryPaymentsServiceTest {

  @InjectMocks
  private QueryPaymentsService queryPaymentsService;

  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private ReceiptMapper receiptMapperMock;
  @Mock
  private SessionIdMapper sessionIdMapperMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private ReceiptService receiptServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private String accessToken;
  private UserInfo userInfo;
  private Organization org;
  private PaymentStatusRequest request;
  private List<Long> installmentIds;
  private List<Pair<DebtPositionDTO, InstallmentDTO>> pairList;

  @BeforeEach
  void init() {
    accessToken = "accessToken";
    org = podamFactory.manufacturePojo(Organization.class);
    org.setStatus(OrganizationStatus.ACTIVE);
    userInfo = AuthorizationServiceTest.buildAdminUser(org.getOrganizationId(), org.getOrgFiscalCode(), org.getIpaCode());
    installmentIds = List.of(1L);
    DebtPositionDTO dp = podamFactory.manufacturePojo(DebtPositionDTO.class);
    dp.setOrganizationId(org.getOrganizationId());
    dp.setStatus(DebtPositionStatus.PAID);
    InstallmentDTO inst = dp.getPaymentOptions().getFirst().getInstallments().getFirst();
    inst.setInstallmentId(1L);
    inst.setStatus(InstallmentStatus.PAID);
    pairList = List.of(Pair.of(dp, inst));
  }

  @ParameterizedTest
  @EnumSource(QueryPaymentStatusType.class)
  void testProcessRequestOk(QueryPaymentStatusType idType) {
    // Given
    byte[] encodedReceipt = "encodedReceipt".getBytes();
    InstallmentDTO installment = pairList.getFirst().getRight();
    DebtPositionDTO debtPosition = pairList.getFirst().getLeft();
    ReceiptWithAdditionalNodeDataDTO receiptDTO = podamFactory.manufacturePojo(ReceiptWithAdditionalNodeDataDTO.class);
    request = new PaymentStatusRequest(org.getIpaCode(), idType, installment.getInstallmentId().toString(), true);
    PaymentStatusResponseDTO expectedResponse = new PaymentStatusResponseDTO()
      .status(InstallmentStatus.PAID)
      .receipt(receiptDTO)
      .receiptBytes(new ByteArrayResource(encodedReceipt))
      .paymentId(installment.getInstallmentId().toString())
      .lastUpdateDateTime(installment.getUpdateDate());

    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
    switch (idType) {
      case PAYMENT_ID -> {
        when(sessionIdMapperMock.mapSessionIdToInstallmentIds(request.id())).thenReturn(installmentIds);
        when(debtPositionServiceMock.getDebtPositionDTOByInstallmentId(installment.getInstallmentId(), accessToken))
          .thenReturn(debtPosition);
      }
      case IUD -> {
        when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIud(org.getOrganizationId(), request.id(), InstallmentFacadeService.ALLOWED_ORIGINS, accessToken))
          .thenReturn(List.of(debtPosition));
        installment.setIud(request.id());
      }
      case NOTICE_NUMBER -> {
        when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(org.getOrganizationId(), request.id(), InstallmentFacadeService.ALLOWED_ORIGINS, accessToken))
          .thenReturn(List.of(debtPosition));
        installment.setIuv(request.id());
      }
    }
    when(receiptMapperMock.map2ReceiptWithAdditionalNodeDataDTO(installment, accessToken)).thenReturn(receiptDTO);
    when(receiptServiceMock.getReceiptById(installment.getReceiptId(), org.getOrganizationId(), accessToken)).thenReturn(encodedReceipt);
    // When
    PaymentStatusResponseDTO result = queryPaymentsService.processRequest(request, userInfo, accessToken);
    // Then
    assertNotNull(result);
    assertEquals(expectedResponse, result);
  }

  @Test
  void testProcessRequestFaults() {
    // Given
    request = new PaymentStatusRequest(org.getIpaCode(), PAYMENT_ID, "invalidSessionId", true);

    when(sessionIdMapperMock.mapSessionIdToInstallmentIds(request.id())).thenReturn(List.of());
    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
    // When
    SilFaultException ex = assertThrows(SilFaultException.class, () -> queryPaymentsService.processRequest(request, userInfo, accessToken));
    // Then
    assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO, ex.getFault());
  }

  @Test
  void testProcessRequestUnpaidInstallment() {
    // Given
    InstallmentDTO unpaidInstallment = pairList.getFirst().getRight();
    unpaidInstallment.setStatus(InstallmentStatus.UNPAID);
    DebtPositionDTO debtPosition = pairList.getFirst().getLeft();
    request = new PaymentStatusRequest(org.getIpaCode(), PAYMENT_ID, unpaidInstallment.getInstallmentId().toString(), true);
    PaymentStatusResponseDTO expectedResponse = new PaymentStatusResponseDTO()
      .status(InstallmentStatus.UNPAID)
      .paymentId(unpaidInstallment.getInstallmentId().toString())
      .lastUpdateDateTime(unpaidInstallment.getUpdateDate());

    when(sessionIdMapperMock.mapSessionIdToInstallmentIds(request.id())).thenReturn(installmentIds);
    when(debtPositionServiceMock.getDebtPositionDTOByInstallmentId(unpaidInstallment.getInstallmentId(), accessToken)).thenReturn(debtPosition);
    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
    // When
    PaymentStatusResponseDTO result = queryPaymentsService.processRequest(request, userInfo, accessToken);
    // Then
    assertEquals(expectedResponse, result);
  }

  @Test
  void testProcessRequestExpiredInstallment() {
    // Given
    InstallmentDTO expiredInstallment = pairList.getFirst().getRight();
    expiredInstallment.setStatus(InstallmentStatus.EXPIRED);
    DebtPositionDTO debtPosition = pairList.getFirst().getLeft();
    request = new PaymentStatusRequest(org.getIpaCode(), PAYMENT_ID, expiredInstallment.getInstallmentId().toString(), true);
    PaymentStatusResponseDTO expectedResponse = new PaymentStatusResponseDTO()
      .status(InstallmentStatus.EXPIRED)
      .paymentId(expiredInstallment.getInstallmentId().toString())
      .lastUpdateDateTime(expiredInstallment.getUpdateDate());

    when(sessionIdMapperMock.mapSessionIdToInstallmentIds(request.id())).thenReturn(installmentIds);
    when(debtPositionServiceMock.getDebtPositionDTOByInstallmentId(expiredInstallment.getInstallmentId(), accessToken)).thenReturn(debtPosition);
    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
    // When
    PaymentStatusResponseDTO result = queryPaymentsService.processRequest(request, userInfo, accessToken);
    // Then
    assertEquals(expectedResponse, result);
  }

  @Test
  void testProcessRequestUnpayableInstallment() {
    // Given
    InstallmentDTO unpayableInstallment = pairList.getFirst().getRight();
    unpayableInstallment.setStatus(InstallmentStatus.UNPAYABLE);
    DebtPositionDTO debtPosition = pairList.getFirst().getLeft();
    request = new PaymentStatusRequest(org.getIpaCode(), PAYMENT_ID, unpayableInstallment.getInstallmentId().toString(), true);
    PaymentStatusResponseDTO expectedResponse = new PaymentStatusResponseDTO()
      .status(InstallmentStatus.UNPAYABLE)
      .paymentId(unpayableInstallment.getInstallmentId().toString())
      .lastUpdateDateTime(unpayableInstallment.getUpdateDate());

    when(sessionIdMapperMock.mapSessionIdToInstallmentIds(request.id())).thenReturn(installmentIds);
    when(debtPositionServiceMock.getDebtPositionDTOByInstallmentId(unpayableInstallment.getInstallmentId(), accessToken)).thenReturn(debtPosition);
    when(organizationServiceMock.getOrganizationById(org.getOrganizationId(), accessToken)).thenReturn(Optional.of(org));
    // When
    PaymentStatusResponseDTO result = queryPaymentsService.processRequest(request, userInfo, accessToken);
    // Then
    assertEquals(expectedResponse, result);
  }
}
