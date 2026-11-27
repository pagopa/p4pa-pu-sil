package it.gov.pagopa.pu.sil.controller.inbound;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.sendnotification.dto.generated.*;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.inbound.send.SendNotificationRetrieverService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendNotificationControllerTest {
  @Mock
  private SendNotificationRetrieverService sendNotificationRetrieverServiceMock;
  @InjectMocks
  private SendNotificationController sendNotificationController;

  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private final Long organizationId = 1L;
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    UserOrganizationRoles userOrganizationRoles = new UserOrganizationRoles();
    userOrganizationRoles.setRoles(List.of("TEST"));
    userOrganizationRoles.setOrganizationId(1L);
    userOrganizationRoles.setOrganizationFiscalCode(orgFiscalCode);
    userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(userOrganizationRoles));
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      sendNotificationRetrieverServiceMock
    );
  }

  @Test
  void givenCreateSendNotificationThenOk() {
    CreateNotificationRequest request = new CreateNotificationRequest();
    CreateNotificationResponse expectedResult = new CreateNotificationResponse();

    when(sendNotificationRetrieverServiceMock.createSendNotification(
      organizationId,
      request,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResult);

    ResponseEntity<CreateNotificationResponse> response = sendNotificationController.createSendNotification(orgFiscalCode, request);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }

  @Test
  void givenDeleteSendNotificationThenOk() {
    String sendNotificationId = "sendNotificationId";

    Mockito.doNothing().when(sendNotificationRetrieverServiceMock).deleteSendNotification(
      sendNotificationId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    );

    ResponseEntity<Void> response = sendNotificationController.deleteSendNotification(orgFiscalCode,sendNotificationId);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void givenCorrectRequestWhenGetSendNotificationThenOk() {
    String sendNotificationId = "sendNotificationId";
    SendNotificationDTO expectedResult = new SendNotificationDTO();

    when(sendNotificationRetrieverServiceMock.getSendNotification(
      sendNotificationId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResult);

    ResponseEntity<SendNotificationDTO> response = sendNotificationController.getSendNotification(orgFiscalCode,sendNotificationId);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult,response.getBody());
  }

  @Test
  void givenNoSendNotificationWhenGetSendNotificationThenNotFound() {
    String sendNotificationId = "sendNotificationId";

    ResponseEntity<SendNotificationDTO> response = sendNotificationController.getSendNotification(orgFiscalCode,sendNotificationId);

    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Assertions.assertNull(response.getBody());
    verify(sendNotificationRetrieverServiceMock).getSendNotification(Mockito.eq(sendNotificationId),Mockito.eq(organizationId),
      Mockito.any(), Mockito.anyString());
  }

  @Test
  void givenCorrectRequestWhenGetLegalFactsThenOk() {
    String sendNotificationId = "sendNotificationId";
    List<LegalFactDTO> expectedResponseBody = List.of(new LegalFactDTO());

    when(sendNotificationRetrieverServiceMock.getLegalFacts(
      sendNotificationId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResponseBody);

    ResponseEntity<List<LegalFactDTO>> actualResponse =
      sendNotificationController.getLegalFacts(
        orgFiscalCode,
        sendNotificationId
      );

    Assertions.assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    Assertions.assertSame(expectedResponseBody, actualResponse.getBody());
  }

  @Test
  void givenEmptyLegalFactListWhenGetLegalFactsThenOkWithEmptyList() {
    // given
    String sendNotificationId = "SEND_NOTIFICATION_ID";

    when(
      sendNotificationRetrieverServiceMock.getLegalFacts(
        Mockito.eq(sendNotificationId),
        Mockito.eq(organizationId),
        Mockito.any(),
        Mockito.anyString()
      )
    ).thenReturn(Collections.emptyList());

    // when
    ResponseEntity<List<LegalFactDTO>> actualResponse =
      sendNotificationController.getLegalFacts(
        orgFiscalCode,
        sendNotificationId
      );

    // then
    Assertions.assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    Assertions.assertNotNull(actualResponse.getBody());
    Assertions.assertTrue(actualResponse.getBody().isEmpty());
  }

  @Test
  void givenCorrectRequestWhenGetLegalFactDownloadMetadataThenOk() {
    String sendNotificationId = "sendNotificationId";
    String legalFactId = "LEGAL_FACT_ID";
    LegalFactDownloadMetadataDTO expectedResponseBody = new LegalFactDownloadMetadataDTO();

    when(sendNotificationRetrieverServiceMock.getLegalFactDownloadMetadata(
      sendNotificationId,
      legalFactId,
      organizationId,
      userInfo,
      "fakeAccessToken"
    )).thenReturn(expectedResponseBody);

    ResponseEntity<LegalFactDownloadMetadataDTO> actualResponse =
      sendNotificationController.getLegalFactDownloadMetadata(
        orgFiscalCode,
        sendNotificationId,
        legalFactId
      );

    Assertions.assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    Assertions.assertSame(expectedResponseBody, actualResponse.getBody());
  }

  @Test
  void givenNoLegalFactDownloadMetadataWhenGetLegalFactDownloadMetadataThenNotFound() {
    // given
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";

    when(
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
        orgFiscalCode,
        sendNotificationId,
        legalFactId
      );

    // then
    Assertions.assertEquals(HttpStatus.NOT_FOUND, actualResponse.getStatusCode());
    Assertions.assertNull(actualResponse.getBody());
  }
}
