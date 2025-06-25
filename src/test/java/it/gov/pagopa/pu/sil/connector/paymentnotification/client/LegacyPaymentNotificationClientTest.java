package it.gov.pagopa.pu.sil.connector.paymentnotification.client;

import it.gov.pagopa.paymentnotification.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.paymentnotification.config.PaymentNotificationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class LegacyPaymentNotificationClientTest {
  @Mock
  private PaymentNotificationApisHolder paymentNotificationApisHolderMock;
  @Mock
  private DefaultApi paymentNotificationLegacyApiClientMock;

  private LegacyPaymentNotificationClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyPaymentNotificationClient(paymentNotificationApisHolderMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(paymentNotificationApisHolderMock);
  }

  @Test
  void whenNotifyPaymentThenInvokeClient() {
    // Given
    String serviceUrl = "http://example.com/service";
    String accessToken = "accessToken";
    PaymentNotification paymentNotification = new PaymentNotification();

    Mockito.when(paymentNotificationApisHolderMock.getPaymentNotificationLegacyApi(accessToken, serviceUrl))
      .thenReturn(paymentNotificationLegacyApiClientMock);
    Mockito.doNothing().when(paymentNotificationLegacyApiClientMock)
      .paymentNotification(paymentNotification);

    // When  Then
    assertDoesNotThrow(() ->
      client.notifyPayment(accessToken, serviceUrl, paymentNotification));
  }
}
