package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;

public interface SendNotificationRetrieverService {
  CreateNotificationResponse createSendNotification(Long organizationId, CreateNotificationRequest body, UserInfo loggedUser, String accessToken);
  void deleteSendNotification(String sendNotificationId, Long organizationId, UserInfo loggedUser, String accessToken);
  SendNotificationDTO getSendNotification(String sendNotificationId, Long organizationId, UserInfo loggedUser, String accessToken);
}
