package it.gov.pagopa.pu.sil.connector.send_notification.client;

import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDownloadMetadataDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactListElementDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.config.SendNotificationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class SendClient {

  private final SendNotificationApisHolder sendNotificationApisHolder;

  public SendClient(SendNotificationApisHolder sendNotificationApisHolder) {
    this.sendNotificationApisHolder = sendNotificationApisHolder;
  }

  public List<LegalFactListElementDTO> getLegalFacts(String sendNotificationId, String accessToken) {
    return sendNotificationApisHolder.getSendApi(accessToken)
      .retrieveLegalFacts(sendNotificationId);
  }

  public LegalFactDownloadMetadataDTO getLegalFactDownloadMetadata(String sendNotificationId, String legalFactId, String accessToken) {
    try {
      return sendNotificationApisHolder.getSendApi(accessToken)
        .retrieveLegalFactDownloadMetadata(sendNotificationId, legalFactId);
    } catch (HttpClientErrorException.NotFound e) {
      log.warn("Legal fact for sendNotificationId {} and legalFactId {} not found", sendNotificationId, legalFactId);
      return null;
    }
  }
}
