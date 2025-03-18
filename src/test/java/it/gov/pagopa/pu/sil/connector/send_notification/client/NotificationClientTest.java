package it.gov.pagopa.pu.sil.connector.send_notification.client;

import it.gov.pagopa.pu.sendnotification.controller.generated.NotificationApi;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sil.connector.send_notification.config.SendNotificationApisHolder;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationClientTest {

  @Mock
  private SendNotificationApisHolder sendNotificationApisHolderMock;
  @Mock
  private NotificationApi notificationApiMock;

  private NotificationClient notificationClient;

  @BeforeEach
  void setUp() {
    notificationClient = new NotificationClient(sendNotificationApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(sendNotificationApisHolderMock);
  }

  @Test
  void whenCreateSendNotificationThenInvokeWithAccessToken() {
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    CreateNotificationRequest request = new CreateNotificationRequest();
    CreateNotificationResponse expectedResult = new CreateNotificationResponse();

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    when(notificationApiMock.createSendNotification(organizationId,request))
      .thenReturn(expectedResult);

    CreateNotificationResponse result = notificationClient.createSendNotification(
      organizationId, request, accessToken);

    assertSame(expectedResult, result);
  }

  @Test
  void whenDeleteSendNotificationThenInvokeWithAccessToken() {
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    doNothing().when(notificationApiMock).deleteSendNotification(sendNotificationId,organizationId);

    notificationClient.deleteSendNotification(
      sendNotificationId,organizationId, accessToken);

    Mockito.verifyNoMoreInteractions(sendNotificationApisHolderMock,notificationApiMock);
  }

}
