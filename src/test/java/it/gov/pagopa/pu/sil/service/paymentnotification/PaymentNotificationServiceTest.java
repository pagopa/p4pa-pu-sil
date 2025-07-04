package it.gov.pagopa.pu.sil.service.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.service.AccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {
  @Mock
  private OrgSilServiceComponent orgSilServiceComponentMock;
  @Mock
  LegacyPaymentNotificationService legacyPaymentNotificationServiceMock;
  @Mock
  private AccessTokenService accessTokenServiceMock;
  @Mock
  private OrganizationService organizationServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private PagatiMapper pagatiMapperMock;
  @Mock
  private ReceiptService receiptServiceMock;

  @InjectMocks
  private PaymentNotificationService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new PaymentNotificationService(
            orgSilServiceComponentMock,
            legacyPaymentNotificationServiceMock,
            accessTokenServiceMock,
            organizationServiceMock,
            debtPositionServiceMock,
            pagatiMapperMock,
            receiptServiceMock
    );
  }

  @Test
  void whenNotifyPaymentThenOk() {
    byte[] encodedPagati = "pagati".getBytes();
    byte[] encodedReceipt = "receipt".getBytes();
    Long organizationId = 2L;
    Long orgSilServiceId = 1L;
    String nav = "NAV123";
    UserInfo loggedUser = mock(UserInfo.class);
    String token = "token";
    AccessToken accessToken = new AccessToken()
      .accessToken("token")
      .tokenType("Bearer");
    OrgSilService orgSilService = new OrgSilService()
      .organizationId(organizationId)
      .orgSilServiceId(orgSilServiceId)
      .flagLegacy(true)
      .serviceUrl("http://service.url");
    PaymentNotification paymentNotification = new PaymentNotification()
      .rt(Base64.getEncoder().encodeToString(encodedReceipt))
      .esito(Base64.getEncoder().encodeToString(encodedPagati));
    Organization organization = mock(Organization.class);
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPositionDTO.setOrganizationId(organization.getOrganizationId());
    debtPositionDTO.setStatus(DebtPositionStatus.PAID);
    InstallmentDTO installmentDTO = debtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst();
    installmentDTO.setInstallmentId(1l);
    installmentDTO.setStatus(InstallmentStatus.PAID);
    List<InstallmentDTO> installmentDTOs = List.of(installmentDTO);

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilService.getOrgSilServiceId(), accessToken.getAccessToken()))
      .thenReturn(Optional.of(orgSilService));
    when(organizationServiceMock.getOrganizationById(orgSilService.getOrganizationId(), accessToken.getAccessToken()))
        .thenReturn(Optional.of(organization));
    when(debtPositionServiceMock.getInstallmentsByOrganizationIdAndNav(
        orgSilService.getOrganizationId(), nav, null, accessToken.getAccessToken()))
      .thenReturn(installmentDTOs);
    when(debtPositionServiceMock.getDebtPositionByInstallmentId(installmentDTO.getInstallmentId(), accessToken.getAccessToken()))
      .thenReturn(debtPositionDTO);
    when(pagatiMapperMock.mapDebtPositionsToEncodedPagati(debtPositionDTO, installmentDTO, organization, accessToken.getAccessToken()))
      .thenReturn(encodedPagati);
    when(receiptServiceMock.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken.getAccessToken()))
      .thenReturn(encodedReceipt);
    when(accessTokenServiceMock.getSilAccessToken(orgSilService, token))
      .thenReturn(accessToken.getAccessToken());
    doNothing().when(legacyPaymentNotificationServiceMock)
      .notifyPayment(accessToken.getAccessToken(), orgSilService.getServiceUrl(), paymentNotification);
    try (MockedStatic<AuthorizationService> authService = mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).then(Answers.RETURNS_DEEP_STUBS);
      assertDoesNotThrow(() -> service.notifyPayment(orgSilServiceId, nav, loggedUser, token));
    }
  }

  @Test
  void whenInstallmentNotFoundThenPaymentNotFoundException() {
    Long organizationId = 2L;
    Long orgSilServiceId = 1L;
    String nav = "NAV123";
    UserInfo loggedUser = mock(UserInfo.class);
    String token = "token";
    AccessToken accessToken = new AccessToken()
      .accessToken("token")
      .tokenType("Bearer");
    OrgSilService orgSilService = new OrgSilService()
      .organizationId(organizationId)
      .orgSilServiceId(orgSilServiceId)
      .flagLegacy(true)
      .serviceUrl("http://service.url");

    Organization organization = mock(Organization.class);
    InstallmentDTO installmentDTO = mock(InstallmentDTO.class);
    List<InstallmentDTO> installmentDTOs = List.of(installmentDTO);
    DebtPositionDTO debtPositionDTO = mock(DebtPositionDTO.class);

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilService.getOrgSilServiceId(), accessToken.getAccessToken()))
      .thenReturn(Optional.of(orgSilService));
    when(organizationServiceMock.getOrganizationById(orgSilService.getOrganizationId(), accessToken.getAccessToken()))
      .thenReturn(Optional.of(organization));
    when(debtPositionServiceMock.getInstallmentsByOrganizationIdAndNav(
      orgSilService.getOrganizationId(), nav, null, accessToken.getAccessToken()))
      .thenReturn(installmentDTOs);
    when(debtPositionServiceMock.getDebtPositionByInstallmentId(installmentDTO.getInstallmentId(), accessToken.getAccessToken()))
      .thenReturn(debtPositionDTO);

    try (MockedStatic<AuthorizationService> authService = mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).then(Answers.RETURNS_DEEP_STUBS);
      assertThrows(PaymentNotFoundException.class, () -> service.notifyPayment(orgSilServiceId, nav, loggedUser, token));
    }
  }

  @Test
  void whenOrgSilServiceNotFoundThenThrowsIllegalArgumentException() {
    Long orgSilServiceId = 1L;
    String nav = "NAV123";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "token";

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken)).thenReturn(Optional.empty());

    Assertions.assertThrows(IllegalArgumentException.class, () ->
      service.notifyPayment(orgSilServiceId, nav, loggedUser, accessToken)
    );
  }

  @Test
  void whenValidateUserForOrganizationIdFailsThenAuthorizationDeniedException() {
    Long orgSilServiceId = 1L;
    Long organizationId = 2L;
    String nav = "NAV123";
    String token = "token";
    UserInfo loggedUser = mock(UserInfo.class);
    OrgSilService orgSilService = new OrgSilService().organizationId(organizationId).orgSilServiceId(orgSilServiceId).flagLegacy(true).serviceUrl("http://service.url");
    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, token)).thenReturn(Optional.of(orgSilService));
    try (MockedStatic<AuthorizationService> authService = mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenThrow(AuthorizationDeniedException.class);
      assertThrows(AuthorizationDeniedException.class, () -> service.notifyPayment(orgSilServiceId, nav, loggedUser, token));
    }
  }
}
