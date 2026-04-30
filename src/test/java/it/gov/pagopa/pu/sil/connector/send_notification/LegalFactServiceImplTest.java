package it.gov.pagopa.pu.sil.connector.send_notification;

import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDownloadMetadataDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.client.NotificationClient;
import it.gov.pagopa.pu.sil.connector.send_notification.client.SendClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class LegalFactServiceImplTest {
  @Mock
  private SendClient sendClientMock;
  @Mock
  private NotificationClient notificationClient;

  private LegalFactService legalFactService;

  @BeforeEach
  void setUp() {
    legalFactService = new LegalFactServiceImpl(sendClientMock, notificationClient);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      sendClientMock,
      notificationClient
    );
  }

  @Test
  void whenGetLegalFactsThenInvokeClient(){
    String accessToken = "access_token";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    List<LegalFactDTO> expectedResult = List.of(new LegalFactDTO());
    Mockito.when(notificationClient.getLegalFacts(sendNotificationId, accessToken))
      .thenReturn(expectedResult);

    List<LegalFactDTO> actualResult = legalFactService.getLegalFacts(sendNotificationId, accessToken);

    Assertions.assertSame(expectedResult, actualResult);
  }

  @Test
  void whenGetLegalFactDownloadMetadataThenInvokeClient(){
    String accessToken = "access_token";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";
    LegalFactDownloadMetadataDTO expectedResult = new LegalFactDownloadMetadataDTO();
    Mockito.when(sendClientMock.getLegalFactDownloadMetadata(sendNotificationId, legalFactId, accessToken))
      .thenReturn(expectedResult);

    LegalFactDownloadMetadataDTO actualResult = legalFactService.getLegalFactDownloadMetadata(sendNotificationId, legalFactId, accessToken);

    Assertions.assertSame(expectedResult, actualResult);
  }
}
