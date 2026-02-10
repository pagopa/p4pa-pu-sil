package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDownloadMetadataDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactListElementDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.client.NotificationClient;
import it.gov.pagopa.pu.sil.connector.send_notification.client.SendClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegalFactServiceImpl implements LegalFactService {
  private final SendClient sendClient;
  private final NotificationClient notificationClient;

  public LegalFactServiceImpl(SendClient sendClient, NotificationClient notificationClient) {
    this.sendClient = sendClient;
    this.notificationClient = notificationClient;
  }

  @Override
  public List<LegalFactDTO> getLegalFacts(String sendNotificationId, String accessToken) {
    return notificationClient.getLegalFacts(sendNotificationId, accessToken);
  }

  @Override
  public LegalFactDownloadMetadataDTO getLegalFactDownloadMetadata(String sendNotificationId, String legalFactId, String accessToken) {
    return sendClient.getLegalFactDownloadMetadata(sendNotificationId, legalFactId, accessToken);
  }
}
