package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sil.connector.send_notification.client.NotificationClient;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService{
  private final NotificationClient notificationClient;

  public NotificationServiceImpl(NotificationClient notificationClient) {
    this.notificationClient = notificationClient;
  }

  @Override
  public CreateNotificationResponse createSendNotification( Long organizationId,
    CreateNotificationRequest createNotificationRequest, String accessToken) {
    return notificationClient.createSendNotification(organizationId,createNotificationRequest,accessToken);
  }
}
