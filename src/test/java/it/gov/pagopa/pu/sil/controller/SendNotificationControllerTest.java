package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.*;
import it.gov.pagopa.pu.sil.service.notification.SendNotificationRetrieverService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class SendNotificationControllerTest {
  @Mock
  private SendNotificationRetrieverService sendNotificationRetrieverServiceMock;
  @InjectMocks
  private SendNotificationController sendNotificationController;
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = new UserInfo();
    userInfo.setMappedExternalUserId("fakeExternalUser");
    Authentication authentication = new UsernamePasswordAuthenticationToken(userInfo, "fakeAccessToken");
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      sendNotificationRetrieverServiceMock
    );
  }

  @Test
  void givenCreateSendNotificationThenOk() {
    Long organizationId = 1L;
    CreateNotificationRequest request = new CreateNotificationRequest();
    CreateNotificationResponse expectedResult = new CreateNotificationResponse();

    Mockito.when(sendNotificationRetrieverServiceMock.createSendNotification(
      organizationId,
      request,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResult);

    ResponseEntity<CreateNotificationResponse> response = sendNotificationController.createSendNotification(organizationId,request);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }

  @Test
  void givenDeleteSendNotificationThenOk() {
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";

    Mockito.doNothing().when(sendNotificationRetrieverServiceMock).deleteSendNotification(
      sendNotificationId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    );

    ResponseEntity<Void> response = sendNotificationController.deleteSendNotification(organizationId,sendNotificationId);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void givenCorrectRequestWhenGetSendNotificationThenOk() {
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";
    SendNotificationDTO expectedResult = new SendNotificationDTO();

    Mockito.when(sendNotificationRetrieverServiceMock.getSendNotification(
      sendNotificationId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResult);

    ResponseEntity<SendNotificationDTO> response = sendNotificationController.getSendNotification(organizationId,sendNotificationId);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult,response.getBody());
  }

  @Test
  void givenNoSendNotificationWhenGetSendNotificationThenNotFound() {
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";

    ResponseEntity<SendNotificationDTO> response = sendNotificationController.getSendNotification(organizationId,sendNotificationId);

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Assertions.assertNull(response.getBody());
    Mockito.verify(sendNotificationRetrieverServiceMock).getSendNotification(Mockito.eq(sendNotificationId),Mockito.eq(organizationId),
      Mockito.any(), Mockito.anyString());
  }

  @Test
  void givenCorrectRequestWhenGetLegalFactsThenOk() {
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";
    List<LegalFactListElementDTO> expectedResponseBody = List.of(new LegalFactListElementDTO());

    Mockito.when(sendNotificationRetrieverServiceMock.getLegalFacts(
      sendNotificationId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResponseBody);

    ResponseEntity<List<LegalFactListElementDTO>> actualResponse =
      sendNotificationController.getLegalFacts(
        organizationId,
        sendNotificationId
      );

    Assertions.assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    Assertions.assertSame(expectedResponseBody, actualResponse.getBody());
  }

  @Test
  void givenNoLegalFactsWhenGetLegalFactsThenNotFound() {
    // given
    Long organizationId = 1L;
    String sendNotificationId = "SEND_NOTIFICATION_ID";

    Mockito.when(
      sendNotificationRetrieverServiceMock.getLegalFacts(
        Mockito.eq(sendNotificationId),
        Mockito.eq(organizationId),
        Mockito.any(),
        Mockito.anyString()
      )
    ).thenReturn(null);

    // when
    ResponseEntity<List<LegalFactListElementDTO>> actualResponse =
      sendNotificationController.getLegalFacts(
        organizationId,
        sendNotificationId
      );

    // then
    Assertions.assertEquals(HttpStatus.NOT_FOUND, actualResponse.getStatusCode());
    Assertions.assertNull(actualResponse.getBody());
  }

  @Test
  void givenCorrectRequestWhenGetLegalFactDownloadMetadataThenOk() {
    Long organizationId = 1L;
    String sendNotificationId = "sendNotificationId";
    String legalFactId = "LEGAL_FACT_ID";
    LegalFactDownloadMetadataDTO expectedResponseBody = new LegalFactDownloadMetadataDTO();

    Mockito.when(sendNotificationRetrieverServiceMock.getLegalFactDownloadMetadata(
      sendNotificationId,
      legalFactId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResponseBody);

    ResponseEntity<LegalFactDownloadMetadataDTO> actualResponse =
      sendNotificationController.getLegalFactDownloadMetadata(
        organizationId,
        sendNotificationId,
        legalFactId
      );

    Assertions.assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    Assertions.assertSame(expectedResponseBody, actualResponse.getBody());
  }

  @Test
  void givenNoLegalFactDownloadMetadataWhenGetLegalFactDownloadMetadataThenNotFound() {
    // given
    Long organizationId = 1L;
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";

    Mockito.when(
      sendNotificationRetrieverServiceMock.getLegalFactDownloadMetadata(
        Mockito.eq(sendNotificationId),
        Mockito.eq(legalFactId),
        Mockito.eq(organizationId),
        Mockito.any(),
        Mockito.anyString()
      )
    ).thenReturn(null);

    // when
    ResponseEntity<LegalFactDownloadMetadataDTO> actualResponse =
      sendNotificationController.getLegalFactDownloadMetadata(
        organizationId,
        sendNotificationId,
        legalFactId
      );

    // then
    Assertions.assertEquals(HttpStatus.NOT_FOUND, actualResponse.getStatusCode());
    Assertions.assertNull(actualResponse.getBody());
  }
}
