package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.LimitedTokenRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.auth.AuthnService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ReceiptWithAdditionalNodeDataDTO;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.ReceiptMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import org.junit.jupiter.api.AfterEach;
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

import static it.gov.pagopa.pu.sil.service.querypayments.AbstractDebtorQueryPaymentService.PU_BFF_APP_NAME;
import static it.gov.pagopa.pu.sil.service.querypayments.AbstractDebtorQueryPaymentService.RESOURCE_RECEIPT;
import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtorQueryPaymentServiceTest {

  @Mock private DebtPositionService debtPositionServiceMock;
  @Mock private OrganizationService organizationServiceMock;
  @Mock private AuthorizationService authorizationServiceMock;
  @Mock private ReceiptService receiptServiceMock;
  @Mock private ReceiptMapper receiptMapperMock;
  @Mock private DebtPositionCheckoutService debtPositionCheckoutServiceMock;
  @Mock private AuthnService authnServiceMock;

  private DebtorQueryPaymentService service;

  @BeforeEach
  void setUp() {
    service = new DebtorQueryPaymentService(
      "http://test-url",
      debtPositionServiceMock,
      organizationServiceMock,
      authorizationServiceMock,
      receiptServiceMock,
      debtPositionCheckoutServiceMock,
      receiptMapperMock,
      authnServiceMock
    );
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      debtPositionServiceMock,
      organizationServiceMock,
      authorizationServiceMock,
      receiptServiceMock,
      debtPositionCheckoutServiceMock,
      receiptMapperMock,
      authnServiceMock
    );
  }

  @ParameterizedTest
  @ValueSource(booleans =  {true, false})
  void givenValidRequestWhenProcessRequestThenBuildsPaymentsHistory(boolean useIpaCode) {
    // Given
    String orgIpaCode = useIpaCode ? "IPA123" : null;
    String accessToken = "token";
    String orgAccessToken = "orgToken";
    long orgId = 42L;
    Long brokerId = 100L;
    List<String> debtPositionTypeOrgCodesToExclude = EXCLUDED_DEBT_POSITION_TYPE_CODES;

    AccessToken limitedScopeToken = AccessToken.builder()
      .accessToken("limited-token")
      .tokenType("typ")
      .expiresIn(24 * 60 * 60)
      .build();

    AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest request = new AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest(
      orgIpaCode,
      PersonEntityType.F,
      "RSSMRA80A01H501U",
      InstallmentStatus.PAID,
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
    org.setIpaCode(Optional.ofNullable(orgIpaCode).orElse("orgIpaCode"));

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
    installment.setStatus(InstallmentStatus.PAID);
    installment.setTransfers(List.of(transfer));

    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    paymentOption.setInstallments(List.of(installment));

    DebtPositionDTO dp = new DebtPositionDTO();
    dp.setOrganizationId(orgId);
    dp.setPaymentOptions(List.of(paymentOption));

    byte[] marshalledReceipt = "receipt".getBytes(StandardCharsets.UTF_8);
    ReceiptWithAdditionalNodeDataDTO receipt = new ReceiptWithAdditionalNodeDataDTO();

    OffsetDateTimeIntervalFilter intervalFilter = new OffsetDateTimeIntervalFilter(request.getDateFrom(), request.getDateTo());
    String expectedReceiptUrl = String.format("http://test-url/organization/%d/receipts/%d/pdf?token=%s", orgId, installment.getReceiptId(), limitedScopeToken.getAccessToken());

    // When
    PaymentHistoryResponseDTO response;
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateBrokerAdminRole(userInfo))
        .thenAnswer(inv -> null);
      if (orgIpaCode != null) {
        // Single organization path
        mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode))
          .thenReturn(orgId);
        mockedAuth.when(() -> AuthorizationService.validateOrganizationBrokered(brokerId, userInfo))
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
      when(authnServiceMock.getAccessToken(org.getIpaCode())).thenReturn(orgAccessToken);
      doReturn(limitedScopeToken).when(authorizationServiceMock).requestLimitedToken(LimitedTokenRequest.builder()
        .organizationId(orgId)
        .app(PU_BFF_APP_NAME)
        .resource(RESOURCE_RECEIPT)
        .resourceId(installment.getReceiptId().toString())
        .expireInSeconds(24L * 60 * 60)
        .singleUsage(false)
        .build(), orgAccessToken);

      response = service.processRequest(request, userInfo, accessToken);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(request.getDateTo(), response.getDateTo());
    Assertions.assertNotNull(response.getPayments());
    Assertions.assertEquals(1, response.getPayments().size());

    PaymentHistoryDTO item = response.getPayments().getFirst();
    Assertions.assertEquals(org.getIpaCode(), item.getIpaCode());
    Assertions.assertEquals("Comune di Test", item.getOrgName());
    Assertions.assertEquals(receipt, item.getReceipt());
    Assertions.assertArrayEquals(marshalledReceipt, item.getReceiptBytes());
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

    AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest request = new AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest(
      orgIpaCode,
      PersonEntityType.F,
      "RSSMRA80A01H501U",
      InstallmentStatus.PAID,
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
