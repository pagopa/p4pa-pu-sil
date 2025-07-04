package it.gov.pagopa.pu.sil.connector.paymentnotification;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.paymentnotification.client.LegacyPaymentNotificationClient;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;

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
    PaymentNotification paymentNotification = new PaymentNotification()
      .esito("OK")
      .rt("RT123");
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode("ORG123")
      .eventType(RegistryEventType.SIL_notificaPagamento)
      .build();
    doNothing().when(legacyPaymentNotificationClientMock).notifyPayment(contextData, accessToken, serviceUrl, paymentNotification);

    // When Then
    assertDoesNotThrow(() ->
        legacyPaymentNotificationService.notifyPayment(contextData, accessToken, serviceUrl, paymentNotification)
    );
  }
}
