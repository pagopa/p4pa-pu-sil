package it.gov.pagopa.pu.sil.connector.send_notification.client;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import it.gov.pagopa.pu.sendnotification.controller.generated.NotificationApi;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.config.SendNotificationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

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

  @Test
  void givenNoSendNotificationWhenDeleteSendNotificationThenIllegalArgumentException() {
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";

    Mockito.when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    Mockito.doThrow(
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null))
      .when(notificationApiMock).deleteSendNotification(sendNotificationId,organizationId);

    Assertions.assertThrows(IllegalArgumentException.class,() -> notificationClient.deleteSendNotification(sendNotificationId,organizationId,accessToken));
  }

  @Test
  void whenGetSendNotificationThenInvokeWithAccessToken() {
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";
    SendNotificationDTO expectedResult = new SendNotificationDTO();

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    when(notificationApiMock.getSendNotification(sendNotificationId,organizationId))
      .thenReturn(expectedResult);

    SendNotificationDTO result = notificationClient.getSendNotification(
      sendNotificationId, organizationId, accessToken);

    Assertions.assertSame(expectedResult,result);
  }

  @Test
  void givenNoSendNotificationWhenGetSendNotificationThenNull() {
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";

    Mockito.when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    when(notificationApiMock.getSendNotification(sendNotificationId,organizationId)).thenThrow(
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));

    SendNotificationDTO result = notificationClient.getSendNotification(
      sendNotificationId, organizationId, accessToken);

    Assertions.assertNull(result);
  }
}
