package it.gov.pagopa.pu.sil.service.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.connector.paymentnotification.LegacyPaymentNotificationService;
import it.gov.pagopa.pu.sil.service.AccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {
  @Mock
  private OrgSilServiceComponent orgSilServiceComponentMock;
  @Mock
  LegacyPaymentNotificationService legacyPaymentNotificationServiceMock;
  @Mock
  private AccessTokenService accessTokenServiceMock;

  private PaymentNotificationService service;

  @BeforeEach
  void setUp() {
    service = new PaymentNotificationService(orgSilServiceComponentMock,
      legacyPaymentNotificationServiceMock, accessTokenServiceMock);
  }

  @Test
  void whenNotifyPaymentThenOk() {
    Long organizationId = 2L;
    Long orgSilServiceId = 1L;
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
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
      .rt("RTS123")
      .esito("OK");

    Mockito.when(accessTokenServiceMock.getAccessToken(orgSilService, token))
      .thenReturn(accessToken.getAccessToken());
    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilService.getOrgSilServiceId(), accessToken.getAccessToken()))
      .thenReturn(Optional.of(orgSilService));
    Mockito.doNothing().when(legacyPaymentNotificationServiceMock)
      .notifyPayment(accessToken.getAccessToken(), orgSilService.getServiceUrl(), paymentNotification);

    assertDoesNotThrow(() ->
      service.notifyPayment(orgSilServiceId, nav, loggedUser, token));
  }

  @Test
  void whenOrgSilServiceNotFoundThenThrowsException() {
    Long orgSilServiceId = 1L;
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
    String accessToken = "token";
    Long organizationId = 2L;

    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken)).thenReturn(Optional.empty());

    try (MockedStatic<AuthorizationService> authService = Mockito.mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenAnswer(Answers.RETURNS_DEFAULTS);
      Assertions.assertThrows(IllegalArgumentException.class, () ->
        service.notifyPayment(orgSilServiceId, nav, loggedUser, accessToken)
      );
    }
  }
}
