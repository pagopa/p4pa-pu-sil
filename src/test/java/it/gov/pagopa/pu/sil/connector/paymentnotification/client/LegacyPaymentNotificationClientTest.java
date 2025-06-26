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
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    PaymentNotification paymentNotification = new PaymentNotification()
      .rt("RT123")
      .esito("OK");
    ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();

    Mockito.when(paymentNotificationApisHolderMock.getPaymentNotificationLegacyApi(accessToken, serviceUrl))
      .thenReturn(paymentNotificationLegacyApiClientMock);
    Mockito.when(paymentNotificationLegacyApiClientMock.paymentNotificationWithHttpInfo(paymentNotification))
      .thenReturn(responseEntity);

    // When
    boolean result = client.notifyPayment(accessToken, serviceUrl, paymentNotification);

    // Then
    assertEquals(responseEntity.getStatusCode().is2xxSuccessful(), result);
  }
}
