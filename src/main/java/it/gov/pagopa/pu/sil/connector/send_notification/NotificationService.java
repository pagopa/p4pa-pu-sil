package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;

public interface NotificationService {
  CreateNotificationResponse createSendNotification(CreateNotificationRequest createNotificationRequest, String accessToken);
  void deleteSendNotification(String sendNotificationId, String accessToken);
  SendNotificationDTO getSendNotification(String sendNotificationId, String accessToken);
}
