package it.gov.pagopa.pu.sil.controller;


import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.*;
import it.gov.pagopa.pu.sil.controller.generated.SendNotificationApi;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
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
    String orgFiscalCode, CreateNotificationRequest body) {

    log.info("requested createSendNotification having orgFiscalCode {}", orgFiscalCode);

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);

    return ResponseEntity.ok(
      sendNotificationRetrieverService.createSendNotification(
        organizationId,
        body,
        userInfo,
        accessToken
      )
    );
  }

  @Override
  public ResponseEntity<Void> deleteSendNotification(
    String orgFiscalCode, String sendNotificationId) {

    log.info("requested deleteSendNotification having orgFiscalCode {} and sendNotificationId {}",
      orgFiscalCode,
      sendNotificationId
    );

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);

    sendNotificationRetrieverService.deleteSendNotification(
      sendNotificationId,
      organizationId,
      userInfo,
      accessToken
    );
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<SendNotificationDTO> getSendNotification(
    String orgFiscalCode, String sendNotificationId) {

    log.info("requested getSendNotification having orgFiscalCode {} and sendNotificationId {}",
      orgFiscalCode,
      sendNotificationId
    );

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);

    return ResponseEntity.ofNullable(
      sendNotificationRetrieverService.getSendNotification(
        sendNotificationId,
        organizationId,
        userInfo,
        accessToken
      )
    );
  }

  @Override
  public ResponseEntity<List<LegalFactListElementDTO>> getLegalFacts(
    String orgFiscalCode, String sendNotificationId) {

    log.info("requested getLegalFacts having orgFiscalCode {} and sendNotificationId {}",
      orgFiscalCode,
      sendNotificationId
    );

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);

    return ResponseEntity.ok(
      sendNotificationRetrieverService.getLegalFacts(
        sendNotificationId,
        organizationId,
        userInfo,
        accessToken
      )
    );
  }

  @Override
  public ResponseEntity<LegalFactDownloadMetadataDTO> getLegalFactDownloadMetadata(
    String orgFiscalCode, String sendNotificationId, String legalFactId) {

    log.info("requested getLegalFactDownloadMetadata having orgFiscalCode {}, sendNotificationId {} and legalFactId {}",
      orgFiscalCode,
      sendNotificationId,
      legalFactId
    );

    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, orgFiscalCode);

    return ResponseEntity.ofNullable(
      sendNotificationRetrieverService.getLegalFactDownloadMetadata(
        sendNotificationId,
        legalFactId,
        organizationId,
        userInfo,
        accessToken
      )
    );
  }
}
