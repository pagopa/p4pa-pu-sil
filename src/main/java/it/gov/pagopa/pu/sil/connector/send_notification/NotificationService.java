package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;

public interface NotificationService {
  CreateNotificationResponse createSendNotification(
    Long organizationId, CreateNotificationRequest createNotificationRequest, String accessToken);

  void deleteSendNotification(String sendNotificationId, Long organizationId, String accessToken);
  SendNotificationDTO getSendNotification(String sendNotificationId, Long organizationId, String accessToken);
}
