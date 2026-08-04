package it.gov.pagopa.pu.sil.service.inbound.payments.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.auth.AuthnService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentDTO;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsDTO;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsResponseDTO;
import it.gov.pagopa.pu.sil.mapper.PaymentMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtorQueryUnpaidDebtPositionServiceTest {

  private static final String TRIGGER_PAY_URL = "http://trigger-pay-url";

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Mock private DebtPositionService debtPositionServiceMock;
  @Mock private OrganizationService organizationServiceMock;
  @Mock private AuthorizationService authorizationServiceMock;
  @Mock private PaymentMapper paymentMapperMock;
  @Mock private DebtPositionCheckoutService debtPositionCheckoutServiceMock;
  @Mock private AuthnService authnServiceMock;

  private DebtorQueryUnpaidDebtPositionService service;

  @BeforeEach
  void setUp() {
    service = new DebtorQueryUnpaidDebtPositionService(
      "http://test-url",
      debtPositionServiceMock,
      organizationServiceMock,
      authorizationServiceMock,
      debtPositionCheckoutServiceMock,
      paymentMapperMock,
      authnServiceMock
    );
  }


  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      debtPositionServiceMock,
      organizationServiceMock,
      authorizationServiceMock,
      debtPositionCheckoutServiceMock,
      paymentMapperMock,
      authnServiceMock
    );
  }

  @ParameterizedTest
  @ValueSource(booleans =  {true, false})
  void givenValidRequestWhenProcessRequestThenBuildsOpenPositions(boolean useIpaCode) {
    // Given
    String orgIpaCode = useIpaCode ? "IPA123" : null;
    String accessToken = "token";
    String orgAccessToken = "orgAccessToken";
    long orgId = 42L;
    Long brokerId = 100L;
    List<String> debtPositionTypeOrgCodesToExclude = EXCLUDED_DEBT_POSITION_TYPE_CODES;
    OffsetDateTimeIntervalFilter dateFilter = new OffsetDateTimeIntervalFilter(null, null);

    DebtorQueryPaymentRequest request = new DebtorQueryPaymentRequest(
      orgIpaCode,
      PersonEntityType.F,
      "RSSMRA80A01H501U",
      InstallmentStatus.UNPAID,
      dateFilter.getFrom(),
      dateFilter.getTo()
    );

    UserInfo userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(new UserOrganizationRoles().organizationId(orgId)));
    userInfo.setBrokerId(brokerId);

    Organization org = new Organization();
    org.setOrganizationId(orgId);
    org.setOrgName("Comune di Test");
    org.setStatus(OrganizationStatus.ACTIVE);
    org.setBrokerId(brokerId);
    org.setIpaCode(Optional.ofNullable(orgIpaCode).orElse("orgIpaCode"));

    InstallmentDTO installment = new InstallmentDTO();
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
    installment.setDebtor(debtor);
    installment.setIuv("IUV123");
    installment.setStatus(InstallmentStatus.UNPAID);

    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    paymentOption.setPaymentOptionType(PaymentOptionType.DOWN_PAYMENT);
    paymentOption.setInstallments(List.of(installment));

    DebtPositionDTO dp = new DebtPositionDTO();
    dp.setOrganizationId(orgId);
    dp.setPaymentOptions(List.of(paymentOption));

    PaymentDTO paymentDTO = podamFactory.manufacturePojo(PaymentDTO.class);
    // When
    UnpaidDebtPositionsResponseDTO response;
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateBrokerAdminRole(eq(userInfo)))
        .thenAnswer(inv -> null);

      if (orgIpaCode != null) {
        // Single organization path
        mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode)))
          .thenReturn(orgId);
        mockedAuth.when(() -> AuthorizationService.validateOrganizationBrokered(eq(brokerId), eq(userInfo)))
          .thenAnswer(inv -> null);

        when(organizationServiceMock.getOrganizationById(orgId, accessToken))
          .thenReturn(Optional.of(org));

      } else {
        // Broker organizations path
        when(organizationServiceMock.findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken))
          .thenReturn(List.of(org));

      }
      when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken))
      ).thenReturn(List.of(dp));
      when(authnServiceMock.getAccessToken(org.getIpaCode())).thenReturn(orgAccessToken);

      when(debtPositionCheckoutServiceMock.composeDebtPositionsCheckoutUrl(eq(userInfo.getOrganizations().getFirst().getOrganizationId()), anyString(), eq(null), any(), eq(orgAccessToken)))
        .thenReturn(TRIGGER_PAY_URL);
      when(paymentMapperMock.mapToPaymentDTO(installment, dp, accessToken))
        .thenReturn(paymentDTO);

      response = service.processRequest(request, userInfo, accessToken);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getDebtPositions());
    Assertions.assertEquals(1, response.getDebtPositions().size());

    UnpaidDebtPositionsDTO item = response.getDebtPositions().getFirst();
    Assertions.assertEquals(org.getIpaCode(), item.getIpaCode());
    Assertions.assertEquals("Comune di Test", item.getOrgName());
    Assertions.assertEquals(TRIGGER_PAY_URL, item.getPaymentTriggerUrl());
    Assertions.assertEquals(paymentDTO, item.getUnpaidDebtPosition());


    // Verify interactions
    if (orgIpaCode != null) {
      verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
      verify(organizationServiceMock, never()).findByBrokerIdAndStatus(anyLong(), any(OrganizationStatus.class), anyString());
    } else {
      verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
      verify(organizationServiceMock).findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken);
    }
    verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken)
    );
  }
}
