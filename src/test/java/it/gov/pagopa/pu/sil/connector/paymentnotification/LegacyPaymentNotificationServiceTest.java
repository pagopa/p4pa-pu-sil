package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.paymentnotification.client.LegacyPaymentNotificationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    String accessToken = "accessToken";
    String serviceUrl = "http://example.com/service";
    PaymentNotification paymentNotification = new PaymentNotification();

    // When
    boolean notified = legacyPaymentNotificationService.notifyPayment(accessToken, serviceUrl, paymentNotification);
    // Then
    assertTrue(notified);
  }
}
