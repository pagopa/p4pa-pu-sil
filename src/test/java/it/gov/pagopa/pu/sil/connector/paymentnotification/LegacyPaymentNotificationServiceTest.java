package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.paymentnotification.client.LegacyPaymentNotificationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LegacyPaymentNotificationServiceTest {
  @Mock
  private LegacyPaymentNotificationClient legacyPaymentNotificationClientMock;

  private LegacyPaymentNotificationService legacyPaymentNotificationService;

  @BeforeEach
  void setUp() {
    legacyPaymentNotificationService = new LegacyPaymentNotificationServiceImpl(legacyPaymentNotificationClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(legacyPaymentNotificationClientMock);
  }

  @Test
  void whenNotifyPaymentThenOk() {
    // Given
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = mock(OrgSilServiceDTO.class);
    String nav = "nav123";
    UserInfo loggedUser = mock(UserInfo.class);
    String accessToken = "accessToken";
    PaymentNotification paymentNotification = new PaymentNotification()
      .esito("OK")
      .rt("RT123");

    doNothing().when(legacyPaymentNotificationClientMock).notifyPayment(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, paymentNotification);

    // When Then
    assertDoesNotThrow(() ->
        legacyPaymentNotificationService.notifyPayment(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, paymentNotification)
    );
  }
}
