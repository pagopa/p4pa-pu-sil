package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDownloadMetadataDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactListElementDTO;

import java.util.List;

public interface LegalFactService {
  List<LegalFactDTO> getLegalFacts(String sendNotificationId, String accessToken);
  LegalFactDownloadMetadataDTO getLegalFactDownloadMetadata(String sendNotificationId, String legalFactId, String accessToken);
}
