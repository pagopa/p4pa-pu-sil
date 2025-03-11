package it.gov.pagopa.pu.sil.connector.send_notification.client;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sil.connector.send_notification.config.SendNotificationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationClient {

  private final SendNotificationApisHolder sendNotificationApisHolder;

  public NotificationClient(SendNotificationApisHolder sendNotificationApisHolder) {
    this.sendNotificationApisHolder = sendNotificationApisHolder;
  }

  public CreateNotificationResponse createSendNotification(Long organizationId, CreateNotificationRequest createNotificationRequest, String accessToken) {
    return sendNotificationApisHolder.getNotificationApi(accessToken)
      .createSendNotification(organizationId, createNotificationRequest);
  }

}
