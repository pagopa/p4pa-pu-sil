package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.client.NotificationClient;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService{
  private final NotificationClient notificationClient;

  public NotificationServiceImpl(NotificationClient notificationClient) {
    this.notificationClient = notificationClient;
  }

  @Override
  public CreateNotificationResponse createSendNotification(CreateNotificationRequest createNotificationRequest, String accessToken) {
    return notificationClient.createSendNotification(createNotificationRequest,accessToken);
  }

  @Override
  public void deleteSendNotification(String sendNotificationId, String accessToken) {
    notificationClient.deleteSendNotification(sendNotificationId,accessToken);
  }

  @Override
  public SendNotificationDTO getSendNotification(String sendNotificationId, String accessToken) {
    return notificationClient.getSendNotification(sendNotificationId,accessToken);
  }
}
