package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.*;

import java.util.List;

public interface SendNotificationRetrieverService {
  CreateNotificationResponse createSendNotification(Long organizationId, CreateNotificationRequest body, UserInfo loggedUser, String accessToken);
  void deleteSendNotification(String sendNotificationId, Long organizationId, UserInfo loggedUser, String accessToken);
  SendNotificationDTO getSendNotification(String sendNotificationId, Long organizationId, UserInfo loggedUser, String accessToken);
  List<LegalFactListElementDTO> getLegalFacts(String sendNotificationId, Long organizationId, UserInfo loggedUser, String accessToken);
  LegalFactDownloadMetadataDTO getLegalFactDownloadMetadata(String sendNotificationId, String legalFactId, Long organizationId, UserInfo loggedUser, String accessToken);
}
