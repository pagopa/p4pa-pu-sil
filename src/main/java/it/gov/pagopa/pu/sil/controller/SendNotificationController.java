package it.gov.pagopa.pu.sil.controller;


import it.gov.pagopa.pu.sendnotification.dto.generated.*;
import it.gov.pagopa.pu.sil.controller.generated.SendNotificationApi;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.notification.SendNotificationRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class SendNotificationController implements SendNotificationApi {
  private final SendNotificationRetrieverService sendNotificationRetrieverService;

  public SendNotificationController(
    SendNotificationRetrieverService sendNotificationRetrieverService) {
    this.sendNotificationRetrieverService = sendNotificationRetrieverService;
  }

  @Override
  public ResponseEntity<CreateNotificationResponse> createSendNotification(
    Long organizationId, CreateNotificationRequest body) {
    log.info("requested createSendNotification having organizationId {}",organizationId);
    return ResponseEntity.ok(sendNotificationRetrieverService.createSendNotification(organizationId,body,
      SecurityUtils.getLoggedUser(),SecurityUtils.getAccessToken()));
  }

  @Override
  public ResponseEntity<Void> deleteSendNotification(
    Long organizationId, String sendNotificationId) {
    log.info("requested deleteSendNotification having organizationId {} and sendNotificationId {}",organizationId, sendNotificationId);
    sendNotificationRetrieverService.deleteSendNotification(sendNotificationId,organizationId,
      SecurityUtils.getLoggedUser(),SecurityUtils.getAccessToken());
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<SendNotificationDTO> getSendNotification(
    Long organizationId,
    String sendNotificationId) {
    log.info("requested getSendNotification having organizationId {} and sendNotificationId {}",organizationId, sendNotificationId);
    return ResponseEntity.ofNullable(sendNotificationRetrieverService.getSendNotification(sendNotificationId,organizationId,
      SecurityUtils.getLoggedUser(),SecurityUtils.getAccessToken()));
  }

  @Override
  public ResponseEntity<List<LegalFactListElementDTO>> getLegalFacts(
    Long organizationId, String sendNotificationId) {
    log.info("requested getLegalFacts having organizationId {} and sendNotificationId {}",
      organizationId,
      sendNotificationId
    );
    List<LegalFactListElementDTO> legalFacts = sendNotificationRetrieverService.getLegalFacts(
      sendNotificationId,
      organizationId,
      SecurityUtils.getLoggedUser(),
      SecurityUtils.getAccessToken()
    );
    return ResponseEntity.ofNullable(legalFacts==null || legalFacts.isEmpty() ? null : legalFacts);
  }

  @Override
  public ResponseEntity<LegalFactDownloadMetadataDTO> getLegalFactDownloadMetadata(
    Long organizationId, String sendNotificationId, String legalFactId) {
    log.info("requested getLegalFactDownloadMetadata having organizationId {}, sendNotificationId {} and legalFactId {}",
      organizationId,
      sendNotificationId,
      legalFactId
    );
    LegalFactDownloadMetadataDTO legalFactDownloadMetadata = sendNotificationRetrieverService.getLegalFactDownloadMetadata(
      sendNotificationId,
      legalFactId,
      organizationId,
      SecurityUtils.getLoggedUser(),
      SecurityUtils.getAccessToken()
    );
    return ResponseEntity.ofNullable(legalFactDownloadMetadata);
  }
}
