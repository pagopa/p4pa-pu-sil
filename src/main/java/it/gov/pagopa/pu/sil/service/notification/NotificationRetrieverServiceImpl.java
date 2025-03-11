package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sil.connector.send_notification.NotificationService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import org.springframework.stereotype.Service;

@Service
public class NotificationRetrieverServiceImpl implements NotificationRetrieverService{
  private final NotificationService notificationService;

  public NotificationRetrieverServiceImpl(
    NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Override
  public CreateNotificationResponse createSendNotification(
    Long organizationId,
    CreateNotificationRequest body,
    UserInfo loggedUser, String accessToken) {
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    return notificationService.createSendNotification(organizationId,body,accessToken);
  }
}
