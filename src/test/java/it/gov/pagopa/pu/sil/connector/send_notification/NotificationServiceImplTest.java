package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.client.NotificationClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
  @Mock
  private NotificationClient notificationClientMock;
  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationServiceImpl(notificationClientMock);
  }

  @Test
  void whenCreateSendNotificationThenInvokeClient(){
    CreateNotificationRequest request = new CreateNotificationRequest();
    String accessToken = "access_token";
    Long organizationId = 1L;
    CreateNotificationResponse expectedResponse = new CreateNotificationResponse();
    Mockito.when(notificationClientMock.createSendNotification(organizationId,request,accessToken)).thenReturn(expectedResponse);

    CreateNotificationResponse response = notificationService.createSendNotification(
      organizationId,request, accessToken);

    Assertions.assertSame(expectedResponse,response);
  }

  @Test
  void whenDeleteSendNotificationThenInvokeClient(){
    String accessToken = "access_token";
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";
    Mockito.doNothing().when(notificationClientMock).deleteSendNotification(sendNotificationId,organizationId,accessToken);

    notificationService.deleteSendNotification(
      sendNotificationId,organizationId, accessToken);

    Mockito.verifyNoMoreInteractions(notificationClientMock);
  }

  @Test
  void whenGetSendNotificationThenInvokeClient(){
    String accessToken = "access_token";
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";
    SendNotificationDTO expectedResult = new SendNotificationDTO();
    Mockito.when(notificationClientMock.getSendNotification(sendNotificationId,organizationId,accessToken)).thenReturn(
      expectedResult);

    SendNotificationDTO result = notificationService.getSendNotification(
      sendNotificationId, organizationId, accessToken);

    Assertions.assertSame(expectedResult,result);
  }
}
