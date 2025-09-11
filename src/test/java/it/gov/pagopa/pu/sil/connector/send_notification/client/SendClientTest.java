package it.gov.pagopa.pu.sil.connector.send_notification.client;

import it.gov.pagopa.pu.sendnotification.controller.generated.SendApi;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDownloadMetadataDTO;
import it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactListElementDTO;
import it.gov.pagopa.pu.sil.connector.send_notification.config.SendNotificationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendClientTest {

  @Mock
  private SendNotificationApisHolder sendNotificationApisHolderMock;
  @Mock
  private SendApi sendApiMock;

  private SendClient sendClient;

  @BeforeEach
  void setUp() {
    sendClient = new SendClient(sendNotificationApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      sendApiMock,
      sendNotificationApisHolderMock
    );
  }

  @Test
  void whenGetLegalFactsThenSuccess() {
    // given
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    List<LegalFactListElementDTO> expectedResult = List.of(new LegalFactListElementDTO());

    when(sendNotificationApisHolderMock.getSendApi(accessToken))
      .thenReturn(sendApiMock);
    when(sendApiMock.retrieveLegalFacts(sendNotificationId))
      .thenReturn(expectedResult);

    // when
    List<LegalFactListElementDTO> actualResult = sendClient.getLegalFacts(
      sendNotificationId, accessToken);

    // then
    Assertions.assertSame(expectedResult, actualResult);
  }

  @Test
  void whenGetLegalFactDownloadMetadataThenSuccess() {
    // given
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";
    LegalFactDownloadMetadataDTO expectedResult = new LegalFactDownloadMetadataDTO();

    when(sendNotificationApisHolderMock.getSendApi(accessToken))
      .thenReturn(sendApiMock);
    when(sendApiMock.retrieveLegalFactDownloadMetadata(sendNotificationId, legalFactId))
      .thenReturn(expectedResult);

    // when
    LegalFactDownloadMetadataDTO actualResult = sendClient.getLegalFactDownloadMetadata(
      sendNotificationId, legalFactId, accessToken);

    //then
    Assertions.assertSame(expectedResult, actualResult);
  }

  @Test
  void givenNotFoundWhenGetLegalFactDownloadMetadataThenNull() {
    // given
    String accessToken = "ACCESSTOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";

    when(sendNotificationApisHolderMock.getSendApi(accessToken))
      .thenReturn(sendApiMock);
    when(sendApiMock.retrieveLegalFactDownloadMetadata(sendNotificationId, legalFactId)).thenThrow(
      HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));

    // when
    LegalFactDownloadMetadataDTO actualResult = sendClient.getLegalFactDownloadMetadata(sendNotificationId, legalFactId, accessToken);

    //then
    Assertions.assertNull(actualResult);
  }
}
