package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;

public interface NotificationRetrieverService {
  CreateNotificationResponse createSendNotification(Long organizationId, CreateNotificationRequest body, UserInfo loggedUser, String accessToken);
}
