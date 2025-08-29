package it.gov.pagopa.pu.sil.connector.paymentnotification.client;

import it.gov.pagopa.paymentnotification.legacy.controller.generated.DefaultApi;
import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.paymentnotification.config.LegacyPaymentNotificationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.util.Utilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LegacyPaymentNotificationClientTest {
  @Mock
  private LegacyPaymentNotificationApisHolder legacyPaymentNotificationApisHolderMock;
  @Mock
  private DefaultApi paymentNotificationLegacyApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private LegacyPaymentNotificationClient client;

  @BeforeEach
  void setUp() {
    client = new LegacyPaymentNotificationClient(legacyPaymentNotificationApisHolderMock, registryLoggerMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(legacyPaymentNotificationApisHolderMock,
                                      paymentNotificationLegacyApiClientMock,
                                      registryLoggerMock);
  }

  @Test
  void whenNotifyPaymentThenInvokeClient() {
    // Given
    String serviceUrl = "http://example.com/";
    String accessToken = "accessToken";
    String orgFiscalCode = "orgFiscalCode";
    OrgSilServiceDTO orgSilServiceDTO = new OrgSilServiceDTO()
      .orgSilServiceId(1L)
      .applicationName("TestApp")
      .serviceUrl(serviceUrl)
      .flagLegacy(true);
    String nav = "31234567890";
    UserInfo loggedUser = mock(UserInfo.class);
    PaymentNotification paymentNotification = new PaymentNotification()
      .rt("RT123")
      .esito("OK");

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_notificaPagamento)
      .orgSilServiceName("TestApp")
      .iuv(Utilities.nav2Iuv(nav))
      .loggedUser(loggedUser)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, paymentNotification, false, false);

    Mockito.when(legacyPaymentNotificationApisHolderMock.getPaymentNotificationLegacyApi(accessToken, serviceUrl))
      .thenReturn(paymentNotificationLegacyApiClientMock);
    Mockito.doNothing().when(paymentNotificationLegacyApiClientMock)
      .paymentNotification(paymentNotification);

    // When Then
    assertDoesNotThrow(() -> client.notifyPayment(orgFiscalCode, orgSilServiceDTO, nav, loggedUser, accessToken, paymentNotification));
  }
}
