package it.gov.pagopa.pu.sil.connector.sil.paymentnotification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.sil.paymentnotification.client.NativePaymentNotificationClient;
import it.gov.pagopa.pu.sil.util.TestUtils;
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
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class NativePaymentNotificationServiceTest {
  @Mock
  private NativePaymentNotificationClient nativePaymentNotificationClientMock;

  private NativePaymentNotificationService nativePaymentNotificationService;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    nativePaymentNotificationService = new NativePaymentNotificationServiceImpl(nativePaymentNotificationClientMock);
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(nativePaymentNotificationClientMock);
  }

  @Test
  void whenNotifyPaymentThenOk() {
    // Given
    OrgSilServiceDTO orgSilServiceDTO = podamFactory.manufacturePojo(OrgSilServiceDTO.class);
    UserInfo loggedUser = podamFactory.manufacturePojo(UserInfo.class);
    String accessToken = "accessToken";
    PaymentNotificationRequest paymentNotificationRequest = podamFactory.manufacturePojo(PaymentNotificationRequest.class);

    doNothing().when(nativePaymentNotificationClientMock).notifyPayment(orgSilServiceDTO, loggedUser, accessToken, paymentNotificationRequest);

    // When Then
    assertDoesNotThrow(() ->
        nativePaymentNotificationService.notifyPayment(orgSilServiceDTO, loggedUser, accessToken, paymentNotificationRequest)
    );
  }
}
