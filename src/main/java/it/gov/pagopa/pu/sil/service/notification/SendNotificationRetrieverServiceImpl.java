package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.NotificationService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SendNotificationRetrieverServiceImpl implements SendNotificationRetrieverService {
  private final NotificationService notificationService;

  public SendNotificationRetrieverServiceImpl(
    NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Override
  public CreateNotificationResponse createSendNotification(
    Long organizationId,
    CreateNotificationRequest body,
    UserInfo loggedUser, String accessToken
  ) {
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    body.setOrganizationId(organizationId);
    return notificationService.createSendNotification(body,accessToken);
  }

  @Override
  public void deleteSendNotification(String sendNotificationId,
    Long organizationId,
    UserInfo loggedUser, String accessToken
  ) {
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    SendNotificationDTO sendNotification = notificationService.getSendNotification(sendNotificationId, accessToken);
    validateSendNotificationOrganization(organizationId, sendNotification);
    notificationService.deleteSendNotification(sendNotificationId,accessToken);
  }

  @Override
  public SendNotificationDTO getSendNotification(String sendNotificationId,
    Long organizationId,
    UserInfo loggedUser, String accessToken
  ) {
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    SendNotificationDTO sendNotification = notificationService.getSendNotification(sendNotificationId, accessToken);
    validateSendNotificationOrganization(organizationId, sendNotification);
    return sendNotification;
  }

  private static void validateSendNotificationOrganization(Long organizationId, SendNotificationDTO sendNotification) {
    if(!sendNotification.getOrganizationId().equals(organizationId)){
      log.info("Requested Notification for organization {}, but the sendNotificationId ({}) is related to organizationId {}", organizationId, sendNotification, sendNotification.getOrganizationId());
      throw new IllegalArgumentException("SendNotification not found");
    }
  }

}
