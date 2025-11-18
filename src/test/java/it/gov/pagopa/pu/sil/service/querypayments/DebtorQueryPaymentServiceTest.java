package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PaymentOptionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.debtpositions.dto.generated.TransferDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptWithAdditionalNodeDataDTO;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.ReceiptMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;

import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtorQueryPaymentServiceTest {

  @Mock private DebtPositionService debtPositionServiceMock;
  @Mock private OrganizationService organizationServiceMock;
  @Mock private ReceiptService receiptServiceMock;
  @Mock private ReceiptMapper receiptMapperMock;

  private DebtorQueryPaymentService service;

  @BeforeEach
  void setUp() {
    service = new DebtorQueryPaymentService(
      "http://test-url",
      debtPositionServiceMock,
      organizationServiceMock,
      receiptServiceMock,
      receiptMapperMock
    );
  }

  @ParameterizedTest
  @ValueSource(booleans =  {true, false})
  void givenValidRequestWhenProcessRequestThenBuildsPaymentsHistory(boolean useIpaCode) {
    // Given
    String orgIpaCode = useIpaCode ? "IPA123" : null;
    String accessToken = "token";
    long orgId = 42L;
    Long brokerId = 100L;
    List<String> debtPositionTypeOrgCodesToExclude = EXCLUDED_DEBT_POSITION_TYPE_CODES;

    DebtorQueryPaymentRequest request = new DebtorQueryPaymentRequest(
      orgIpaCode,
      PersonEntityType.F,
      "RSSMRA80A01H501U",
      OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS),
      OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS)
    );

    UserInfo userInfo = new UserInfo();
    userInfo.setBrokerId(brokerId);

    Organization org = new Organization();
    org.setOrganizationId(orgId);
    org.setOrgName("Comune di Test");
    org.setOrgFiscalCode("11111111111");
    org.setStatus(OrganizationStatus.ACTIVE);
    org.setBrokerId(brokerId);
    Optional.ofNullable(orgIpaCode).ifPresent(org::setIpaCode);

    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode("RSSMRA80A01H501U");
    debtor.setFullName("Mario Rossi");
    debtor.setAddress("Via Roma 1");
    debtor.setCivic("1");
    debtor.setPostalCode("00100");
    debtor.setLocation("Roma");
    debtor.setProvince("RM");
    debtor.setNation("IT");
    debtor.setEmail("mario.rossi@example.com");
    debtor.setEntityType(PersonEntityType.F);

    TransferDTO transfer = new TransferDTO();
    transfer.setTransferIndex(1);
    transfer.setAmountCents(12345L);
    transfer.setRemittanceInformation("Causale versamento");
    transfer.setMbdAttachment("allegato-mbd");

    InstallmentDTO installment = new InstallmentDTO();
    installment.setDebtor(debtor);
    installment.setIud("IUD-001");
    installment.setLegacyPaymentMetadata("MBDAAA|MBDBBB");
    installment.setReceiptId(123L);
    installment.setTransfers(List.of(transfer));

    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    paymentOption.setInstallments(List.of(installment));

    DebtPositionDTO dp = new DebtPositionDTO();
    dp.setOrganizationId(orgId);
    dp.setPaymentOptions(List.of(paymentOption));

    byte[] marshalledReceipt = "receipt".getBytes(StandardCharsets.UTF_8);
    ReceiptWithAdditionalNodeDataDTO receipt = new ReceiptWithAdditionalNodeDataDTO();

    OffsetDateTimeIntervalFilter intervalFilter = new OffsetDateTimeIntervalFilter(request.dateFrom(), request.dateTo());

    // When
    PaymentHistoryResponseDTO response;
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateBrokerAdminRole(userInfo))
        .thenAnswer(inv -> null);
      if (orgIpaCode != null) {
        // Single organization path
        mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode))
          .thenReturn(orgId);
        mockedAuth.when(() -> AuthorizationService.isOrganizationHandledByBroker(brokerId, userInfo))
          .thenAnswer(inv -> null);

        when(organizationServiceMock.getOrganizationById(orgId, accessToken))
          .thenReturn(Optional.of(org));

      } else {
        when(organizationServiceMock.findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken))
          .thenReturn(List.of(org));
      }

      when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        "RSSMRA80A01H501U",
        PersonEntityType.F,
        List.of(orgId),
        debtPositionTypeOrgCodesToExclude,
        InstallmentStatus.PAID,
        intervalFilter,
        accessToken
      )).thenReturn(List.of(dp));

      when(receiptServiceMock.getReceiptById(installment.getReceiptId(), orgId, accessToken))
        .thenReturn(marshalledReceipt);
      when(receiptMapperMock.map2ReceiptWithAdditionalNodeDataDTO(installment, accessToken))
        .thenReturn(receipt);

      response = service.processRequest(request, userInfo, accessToken);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(request.dateTo(), response.getDateTo());
    Assertions.assertNotNull(response.getPayments());
    Assertions.assertEquals(1, response.getPayments().size());

    PaymentHistoryDTO item = response.getPayments().getFirst();
    Assertions.assertEquals(orgIpaCode, item.getIpaCode());
    Assertions.assertEquals("Comune di Test", item.getOrgName());
    Assertions.assertEquals(receipt, item.getReceipt());
    Assertions.assertArrayEquals(marshalledReceipt, item.getReceiptBytes());
    String expectedReceiptUrl = String.format("http://test-url/organization/%d/rt/%d", orgId, installment.getReceiptId());
    Assertions.assertEquals(expectedReceiptUrl, item.getReceiptDownloadUrl());

    // Verify interactions
    if (orgIpaCode != null) {
      verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
      verify(organizationServiceMock, never()).findByBrokerIdAndStatus(anyLong(), any(OrganizationStatus.class), anyString());
    } else {
      verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
      verify(organizationServiceMock).findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken);
    }
    verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      "RSSMRA80A01H501U",
      PersonEntityType.F,
      List.of(orgId),
      debtPositionTypeOrgCodesToExclude,
      InstallmentStatus.PAID,
      intervalFilter,
      accessToken
    );
  }

  @Test
  void givenInactiveOrganizationWhenProcessRequestThenThrowsSilFaultException() {
    // Given
    String accessToken = "token";
    String orgIpaCode = "IPA123";
    long orgId = 42L;
    Long brokerId = 100L;

    DebtorQueryPaymentRequest request = new DebtorQueryPaymentRequest(
      orgIpaCode,
      PersonEntityType.F,
      "RSSMRA80A01H501U",
      OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS),
      OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS)
    );

    UserInfo userInfo = new UserInfo();
    userInfo.setBrokerId(brokerId);

    when(organizationServiceMock.getOrganizationById(orgId, accessToken))
      .thenReturn(Optional.empty());

    // When - Then
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateBrokerAdminRole(eq(userInfo)))
        .thenAnswer(inv -> null);
      mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode)))
        .thenReturn(orgId);

      Assertions.assertThrows(SilFaultException.class, () ->
        service.processRequest(request, userInfo, accessToken)
      );
    }

    verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
    verifyNoInteractions(debtPositionServiceMock, receiptServiceMock, receiptMapperMock);
  }
}
