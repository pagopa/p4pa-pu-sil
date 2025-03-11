package it.gov.pagopa.pu.sil.controller;


import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sil.controller.generated.NotificationApi;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.notification.NotificationRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class NotificationController implements NotificationApi {
  private final NotificationRetrieverService notificationRetrieverService;

  public NotificationController(
    NotificationRetrieverService notificationRetrieverService) {
    this.notificationRetrieverService = notificationRetrieverService;
  }

  @Override
  public ResponseEntity<CreateNotificationResponse> createSendNotification(
    Long organizationId, CreateNotificationRequest body) {
    log.info("requested createSendNotification having organizationId {}",organizationId);
    return ResponseEntity.ok(notificationRetrieverService.createSendNotification(organizationId,body,
      SecurityUtils.getLoggedUser(),SecurityUtils.getAccessToken()));
  }
}
