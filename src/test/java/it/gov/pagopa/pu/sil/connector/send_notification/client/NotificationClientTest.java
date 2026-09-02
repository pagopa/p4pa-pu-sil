package it.gov.pagopa.pu.sil.connector.send_notification.client;

import it.gov.pagopa.pu.sendnotification.client.generated.NotificationApi;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.config.SendNotificationApisHolder;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

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
    CreateNotificationRequest request = new CreateNotificationRequest();
    CreateNotificationResponse expectedResult = new CreateNotificationResponse();

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    when(notificationApiMock.createSendNotification(request))
      .thenReturn(expectedResult);

    CreateNotificationResponse result = notificationClient.createSendNotification(
      request, accessToken);

    assertSame(expectedResult, result);
  }

  @Test
  void whenDeleteSendNotificationThenInvokeWithAccessToken() {
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "sendNotificationId";

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    doNothing().when(notificationApiMock).deleteSendNotification(sendNotificationId);

    notificationClient.deleteSendNotification(
      sendNotificationId,accessToken);

    Mockito.verifyNoMoreInteractions(sendNotificationApisHolderMock,notificationApiMock);
  }

  @Test
  void givenNoSendNotificationWhenDeleteSendNotificationThenIllegalArgumentException() {
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "sendNotificationId";

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    doThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"))
      .when(notificationApiMock).deleteSendNotification(sendNotificationId);

    Assertions.assertThrows(IllegalArgumentException.class,() -> notificationClient.deleteSendNotification(sendNotificationId,accessToken));
  }

  @Test
  void whenGetSendNotificationThenInvokeWithAccessToken() {
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "sendNotificationId";
    SendNotificationDTO expectedResult = new SendNotificationDTO();

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    when(notificationApiMock.getSendNotification(sendNotificationId))
      .thenReturn(expectedResult);

    SendNotificationDTO result = notificationClient.getSendNotification(
      sendNotificationId, accessToken);

    Assertions.assertSame(expectedResult,result);
  }

  @Test
  void givenNoSendNotificationWhenGetSendNotificationThenNull() {
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "sendNotificationId";

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    when(notificationApiMock.getSendNotification(sendNotificationId))
      .thenThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"));

    SendNotificationDTO result = notificationClient.getSendNotification(
      sendNotificationId, accessToken);

    Assertions.assertNull(result);
  }

  @Test
  void whenGetLegalFactsThenSuccess() {
    // given
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    List<LegalFactDTO> expectedResult = List.of(new LegalFactDTO());

    when(sendNotificationApisHolderMock.getNotificationApi(accessToken))
      .thenReturn(notificationApiMock);
    when(notificationApiMock.getLegalFacts(sendNotificationId))
      .thenReturn(expectedResult);

    // when
    List<LegalFactDTO> actualResult = notificationClient.getLegalFacts(
      sendNotificationId, accessToken);

    // then
    Assertions.assertSame(expectedResult, actualResult);
  }
}
