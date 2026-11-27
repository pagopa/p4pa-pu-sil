package it.gov.pagopa.pu.sil.service.outbound.paymentnotification;

import it.gov.pagopa.sil.paymentnotification.dto.generated.PaymentDataDTO;
import it.gov.pagopa.sil.paymentnotificationlegacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.sil.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.connector.sil.paymentnotification.NativePaymentNotificationService;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.mapper.PaymentNotificationMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.inbound.payments.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.service.outbound.SilAccessTokenService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {
  @Mock
  private OrgSilServiceComponent orgSilServiceComponentMock;
  @Mock
  LegacyPaymentNotificationService legacyPaymentNotificationServiceMock;
  @Mock
  NativePaymentNotificationService nativePaymentNotificationServiceMock;
  @Mock
  private SilAccessTokenService silAccessTokenServiceMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private PagatiMapper pagatiMapperMock;
  @Mock
  private ReceiptService receiptServiceMock;
  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;
  @Mock
  private PaymentNotificationMapper paymentNotificationMapperMock;

  @InjectMocks
  private PaymentNotificationService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new PaymentNotificationService(
      orgSilServiceComponentMock,
      legacyPaymentNotificationServiceMock,
      nativePaymentNotificationServiceMock,
      silAccessTokenServiceMock,
      organizationServiceMock,
      pagatiMapperMock,
      receiptServiceMock,
      debtPositionServiceMock,
      debtPositionTypeServiceMock,
      paymentNotificationMapperMock
    );
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      orgSilServiceComponentMock,
      legacyPaymentNotificationServiceMock,
      silAccessTokenServiceMock,
      organizationServiceMock,
      pagatiMapperMock,
      receiptServiceMock,
      debtPositionServiceMock
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void whenLegacyNotifyPaymentThenOk(boolean legacy) {
    // Given
    Long organizationId = 1L;
    String orgFiscalCode = "FISCALCODE";
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, orgFiscalCode, "ORGIPACODE");
    byte[] encodedPagati = "pagati".getBytes();
    byte[] encodedReceipt = "receipt".getBytes();
    Long orgSilServiceId = 1L;
    String nav = "30123456789";
    String token = "token";
    AccessToken accessToken = new AccessToken()
      .accessToken("token")
      .tokenType("Bearer");
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO()
      .organizationId(organizationId)
      .orgSilServiceId(orgSilServiceId)
      .flagLegacy(legacy)
      .serviceUrl("http://service.url");
    PaymentNotification paymentNotification = new PaymentNotification()
      .rt(Base64.getEncoder().encodeToString(encodedReceipt))
      .esito(Base64.getEncoder().encodeToString(encodedPagati));
    Organization organization = podamFactory.manufacturePojo(Organization.class);
    organization.setOrgFiscalCode(orgFiscalCode);
    DebtPosition debtPosition = podamFactory.manufacturePojo(DebtPosition.class);
    debtPosition.setOrganizationId(organization.getOrganizationId());
    debtPosition.setStatus(DebtPositionStatus.PAID);

    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setInstallmentId(1L);
    installmentDTO.setNav(nav);
    installmentDTO.setStatus(InstallmentStatus.PAID);

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilService.getOrgSilServiceId(), accessToken.getAccessToken()))
      .thenReturn(Optional.of(orgSilService));
    when(organizationServiceMock.getOrganizationById(orgSilService.getOrganizationId(), accessToken.getAccessToken()))
      .thenReturn(Optional.of(organization));
    when(debtPositionServiceMock.getDebtPositionByInstallmentId(installmentDTO.getInstallmentId(), accessToken.getAccessToken()))
      .thenReturn(debtPosition);
    when(receiptServiceMock.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken.getAccessToken()))
      .thenReturn(encodedReceipt);
    if(legacy) {
      when(pagatiMapperMock.mapDebtPositionsToEncodedPagati(installmentDTO, organization, accessToken.getAccessToken()))
        .thenReturn(encodedPagati);
      when(silAccessTokenServiceMock.getSilAccessToken(organization.getOrgFiscalCode(), nav, loggedUser, orgSilService, token))
        .thenReturn(accessToken.getAccessToken());
      doNothing().when(legacyPaymentNotificationServiceMock)
        .notifyPayment(organization.getOrgFiscalCode(), orgSilService, nav, loggedUser, accessToken.getAccessToken(), paymentNotification);
    } else {
      DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
      PaymentDataDTO paymentDataDTO = podamFactory.manufacturePojo(PaymentDataDTO.class);
      when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByInstallmentId(installmentDTO.getInstallmentId(), accessToken.getAccessToken()))
        .thenReturn(debtPositionTypeOrg);
      when(paymentNotificationMapperMock.mapPaymentData(installmentDTO, orgFiscalCode, debtPositionTypeOrg.getCode(), accessToken.getAccessToken()))
        .thenReturn(paymentDataDTO);
      doNothing().when(nativePaymentNotificationServiceMock)
        .notifyPayment(eq(orgSilService), eq(loggedUser), eq(accessToken.getAccessToken()), argThat(paymentNotificationRequest -> {
          return paymentNotificationRequest.getPaymentData().equals(paymentDataDTO) &&
                 paymentNotificationRequest.getEncodedReceipt().equals(Base64.getEncoder().encodeToString(encodedReceipt));
        }));
    }

    // When, Then
    assertDoesNotThrow(() -> service.notifyPayment(orgSilServiceId, installmentDTO, loggedUser, token));
  }

  @Test
  void whenOrgSilServiceNotFoundThenThrowsIllegalArgumentException() {
    // Given
    Long orgSilServiceId = 1L;
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser();
    String accessToken = "token";

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken))
      .thenReturn(Optional.empty());

    // When, Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      service.notifyPayment(orgSilServiceId, installmentDTO, loggedUser, accessToken)
    );
  }

  @Test
  void whenOrganizationNotFoundThenThrowsIllegalArgumentException() {
    // Given
    Long orgSilServiceId = 1L;
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser();
    String accessToken = "token";

    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO();
    orgSilService.setOrganizationId(1L);

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken))
      .thenReturn(Optional.of(orgSilService));
    when(organizationServiceMock.getOrganizationById(orgSilService.getOrganizationId(), accessToken))
      .thenReturn(Optional.empty());

    // When, Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      service.notifyPayment(orgSilServiceId, installmentDTO, loggedUser, accessToken)
    );
  }

  @Test
  void whenDebtPositionNotFoundThenThrowsIllegalArgumentException() {
    // Given
    Long orgSilServiceId = 1L;
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser();
    String accessToken = "token";

    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO();
    orgSilService.setOrganizationId(1L);

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken))
      .thenReturn(Optional.of(orgSilService));
    when(organizationServiceMock.getOrganizationById(orgSilService.getOrganizationId(), accessToken))
      .thenReturn(Optional.of(new Organization()));
    when(debtPositionServiceMock.getDebtPositionByInstallmentId(installmentDTO.getInstallmentId(), accessToken))
      .thenReturn(null);

    // When, Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      service.notifyPayment(orgSilServiceId, installmentDTO, loggedUser, accessToken)
    );
  }

  @Test
  void whenDebtPositionNotRelatedToOrgSilServiceThenThrowsIllegalArgumentException() {
    // Given
    Long orgSilServiceId = 1L;
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser();
    String accessToken = "token";

    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO();
    orgSilService.setOrganizationId(1L);

    Organization organization = new Organization();
    organization.setOrganizationId(orgSilService.getOrganizationId());

    DebtPosition debtPosition = new DebtPosition();
    debtPosition.setOrganizationId(-1L);

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken))
      .thenReturn(Optional.of(orgSilService));
    when(organizationServiceMock.getOrganizationById(orgSilService.getOrganizationId(), accessToken))
      .thenReturn(Optional.of(organization));
    when(debtPositionServiceMock.getDebtPositionByInstallmentId(installmentDTO.getInstallmentId(), accessToken))
      .thenReturn(debtPosition);

    // When, Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      service.notifyPayment(orgSilServiceId, installmentDTO, loggedUser, accessToken)
    );
  }

  @Test
  void whenValidateUserForOrganizationIdFailsThenAuthorizationDeniedException() {
    // Given
    Long orgSilServiceId = 1L;
    Long organizationId = 1L;
    InstallmentDTO installmentDTO = new InstallmentDTO();
    String token = "token";
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(-1L, "ORGFC", "OTHERIPACODE");
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO().organizationId(organizationId).orgSilServiceId(orgSilServiceId).flagLegacy(true).serviceUrl("http://service.url");

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, token))
      .thenReturn(Optional.of(orgSilService));

    // When, Then
    assertThrows(AuthorizationDeniedException.class, () -> service.notifyPayment(orgSilServiceId, installmentDTO, loggedUser, token));
  }
}
