package it.gov.pagopa.pu.sil.connector.sil.paymentnotification.client;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.sil.paymentnotification.config.PaymentNotificationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.sil.paymentnotification.client.generated.DefaultApi;
import it.gov.pagopa.sil.paymentnotification.dto.generated.PaymentNotificationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationClientTest {
  @Mock
  private PaymentNotificationApisHolder paymentNotificationApisHolderMock;
  @Mock
  private DefaultApi paymentNotificationNativeApiClientMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  private NativePaymentNotificationClient client;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    client = new NativePaymentNotificationClient(paymentNotificationApisHolderMock, registryLoggerMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(paymentNotificationApisHolderMock,
      paymentNotificationNativeApiClientMock,
      registryLoggerMock);
  }

  @Test
  void whenNotifyPaymentThenInvokeClient() {
    // Given
    String accessToken = "accessToken";
    OrgSilServiceDTO orgSilServiceDTO = podamFactory.manufacturePojo(OrgSilServiceDTO.class);
    UserInfo loggedUser = mock(UserInfo.class);
    PaymentNotificationRequest paymentNotification = podamFactory.manufacturePojo(PaymentNotificationRequest.class);

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(paymentNotification.getPaymentData().getOrgFiscalCode())
      .eventType(RegistryEventType.SIL_notificaPagamento)
      .orgSilServiceName(orgSilServiceDTO.getApplicationName())
      .iuv(paymentNotification.getPaymentData().getIuv())
      .loggedUser(loggedUser)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, paymentNotification, false, false);

    when(paymentNotificationApisHolderMock.getPaymentNotificationNativeApi(accessToken, orgSilServiceDTO.getServiceUrl()))
      .thenReturn(paymentNotificationNativeApiClientMock);
    Mockito.doNothing().when(paymentNotificationNativeApiClientMock)
      .paymentNotification(paymentNotification);

    // When Then
    assertDoesNotThrow(() -> client.notifyPayment(orgSilServiceDTO, loggedUser, accessToken, paymentNotification));
  }
}
