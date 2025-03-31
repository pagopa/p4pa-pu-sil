package it.gov.pagopa.pu.sil.connector.send_notification.client;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.config.SendNotificationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
public class NotificationClient {

  private final SendNotificationApisHolder sendNotificationApisHolder;

  public NotificationClient(SendNotificationApisHolder sendNotificationApisHolder) {
    this.sendNotificationApisHolder = sendNotificationApisHolder;
  }

  public CreateNotificationResponse createSendNotification(CreateNotificationRequest createNotificationRequest, String accessToken) {
    return sendNotificationApisHolder.getNotificationApi(accessToken)
      .createSendNotification(createNotificationRequest);
  }

  public void deleteSendNotification(String sendNotificationId, String accessToken) {
    try {
      sendNotificationApisHolder.getNotificationApi(accessToken)
        .deleteSendNotification(sendNotificationId);
    } catch (HttpClientErrorException.NotFound e) {
      throw new IllegalArgumentException(
        "notification with sendNotificationId %s not found".formatted(
          sendNotificationId));
    }
  }

  public SendNotificationDTO getSendNotification(String sendNotificationId, String accessToken) {
    try{
      return sendNotificationApisHolder.getNotificationApi(accessToken)
        .getSendNotification(sendNotificationId);
    } catch (HttpClientErrorException.NotFound e) {
      log.warn("notification with sendNotificationId {} not found", sendNotificationId);
      return null;
    }
  }

}
