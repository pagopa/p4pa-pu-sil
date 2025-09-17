package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.send_notification.LegalFactService;
import it.gov.pagopa.pu.sil.connector.send_notification.NotificationService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SendNotificationRetrieverServiceImpl implements SendNotificationRetrieverService {
  private final NotificationService notificationService;
  private final LegalFactService legalFactService;

  public SendNotificationRetrieverServiceImpl(
    NotificationService notificationService,
    LegalFactService legalFactService) {
    this.notificationService = notificationService;
    this.legalFactService = legalFactService;
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
    if(sendNotification == null || !sendNotification.getOrganizationId().equals(organizationId)){
      String errorMessage = sendNotification == null ?
        "Requested Notification for organization %s, but requested sendNotification is not found".formatted(organizationId) :
        "Requested Notification for organization %s, but the sendNotificationId (%s) is related to organizationId %s".formatted(organizationId, sendNotification, sendNotification.getOrganizationId());
      log.info(errorMessage);
      throw new IllegalArgumentException("SendNotification not found");
    }
  }

  @Override
  public List<LegalFactListElementDTO> getLegalFacts(String sendNotificationId, Long organizationId, UserInfo loggedUser, String accessToken) {
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    SendNotificationDTO sendNotification = notificationService.getSendNotification(sendNotificationId, accessToken);
    validateSendNotificationOrganization(organizationId, sendNotification);
    return legalFactService.getLegalFacts(sendNotificationId, accessToken);
  }

  @Override
  public LegalFactDownloadMetadataDTO getLegalFactDownloadMetadata(String sendNotificationId, String legalFactId, Long organizationId, UserInfo loggedUser, String accessToken) {
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    SendNotificationDTO sendNotification = notificationService.getSendNotification(sendNotificationId, accessToken);
    validateSendNotificationOrganization(organizationId, sendNotification);
    return legalFactService.getLegalFactDownloadMetadata(sendNotificationId, legalFactId, accessToken);
  }
}
