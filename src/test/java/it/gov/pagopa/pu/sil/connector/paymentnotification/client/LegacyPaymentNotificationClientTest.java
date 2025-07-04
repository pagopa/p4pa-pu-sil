package it.gov.pagopa.pu.sil.connector.paymentnotification.client;

import it.gov.pagopa.paymentnotification.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.sil.connector.paymentnotification.config.PaymentNotificationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
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
  @Mock
  private RegistryLogger registryLoggerMock;

  private LegacyPaymentNotificationClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyPaymentNotificationClient(paymentNotificationApisHolderMock, registryLoggerMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(paymentNotificationApisHolderMock,
                                      paymentNotificationLegacyApiClientMock,
                                      registryLoggerMock);
  }

  @Test
  void whenNotifyPaymentThenInvokeClient() {
    // Given
    String serviceUrl = "http://example.com/service";
    String accessToken = "accessToken";
    PaymentNotification paymentNotification = new PaymentNotification()
      .rt("RT123")
      .esito("OK");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode("ORG123")
      .eventType(RegistryEventType.SIL_notificaPagamento)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, contextData, paymentNotification, false, false);

    Mockito.when(paymentNotificationApisHolderMock.getPaymentNotificationLegacyApi(accessToken, serviceUrl))
      .thenReturn(paymentNotificationLegacyApiClientMock);
    Mockito.doNothing().when(paymentNotificationLegacyApiClientMock)
      .paymentNotification(paymentNotification);

    // When Then
    assertDoesNotThrow(() -> client.notifyPayment(contextData, accessToken, serviceUrl, paymentNotification));
  }
}
