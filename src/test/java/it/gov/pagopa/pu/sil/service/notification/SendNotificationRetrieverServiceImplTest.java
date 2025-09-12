package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.*;
import it.gov.pagopa.pu.sil.connector.send_notification.LegalFactService;
import it.gov.pagopa.pu.sil.connector.send_notification.NotificationService;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class SendNotificationRetrieverServiceImplTest {
  @Mock
  private NotificationService notificationServiceMock;
  @Mock
  private LegalFactService legalFactServiceMock;

  private SendNotificationRetrieverService sendNotificationRetrieverService;

  @BeforeEach
  void setUp() {
    sendNotificationRetrieverService = new SendNotificationRetrieverServiceImpl(
      notificationServiceMock,
      legalFactServiceMock
    );
  }

  @AfterEach
  void verifyNotMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      notificationServiceMock,
      legalFactServiceMock
    );
  }

  @Test
  void givenValidUserWhenCreateSendNotificationThenOk() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "ORGIPACODE");
    String accessToken = "TOKEN";
    CreateNotificationRequest request = new CreateNotificationRequest();
    CreateNotificationResponse expectedResponse = new CreateNotificationResponse();

    Mockito.when(notificationServiceMock.createSendNotification(request, accessToken))
      .thenReturn(expectedResponse);

    // When
    CreateNotificationResponse result = sendNotificationRetrieverService.createSendNotification(
      organizationId, request, loggedUser, accessToken);

    // Then
    Assertions.assertSame(expectedResponse, result);
    Assertions.assertEquals(organizationId, request.getOrganizationId());
  }

  @Test
  void givenInvalidUserWhenCreateSendNotificationThenAuthorizationDeniedException() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(-1L, "ORGFC", "ORGIPACODE");
    String accessToken = "TOKEN";
    CreateNotificationRequest request = new CreateNotificationRequest();

    // When, Then
    Assertions.assertThrows(AuthorizationDeniedException.class, () ->
      sendNotificationRetrieverService.createSendNotification(organizationId, request, loggedUser, accessToken));
  }

  @Test
  void givenValidUserWhenDeleteSendNotificationThenOk() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";

    SendNotificationDTO sendNotificationDTO = new SendNotificationDTO();
    sendNotificationDTO.setOrganizationId(organizationId);
    Mockito.when(notificationServiceMock.getSendNotification(sendNotificationId, accessToken)).thenReturn(
      sendNotificationDTO);

    Mockito.doNothing().when(notificationServiceMock).deleteSendNotification(sendNotificationId, accessToken);

    // When
    sendNotificationRetrieverService.deleteSendNotification(
      sendNotificationId, organizationId, loggedUser, accessToken);

    Mockito.verifyNoMoreInteractions(notificationServiceMock);
  }

  @Test
  void givenInvalidOrganizationWhenDeleteSendNotificationThenIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";

    SendNotificationDTO sendNotificationDTO = new SendNotificationDTO();
    sendNotificationDTO.setOrganizationId(2L);
    Mockito.when(notificationServiceMock.getSendNotification(sendNotificationId, accessToken)).thenReturn(
      sendNotificationDTO);


    // When,Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      sendNotificationRetrieverService.deleteSendNotification(sendNotificationId, organizationId, loggedUser, accessToken));
  }

  @Test
  void givenNotFoundNotificationWhenDeleteSendNotificationThenIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";

    Mockito.when(
      notificationServiceMock.getSendNotification(sendNotificationId, accessToken)
    ).thenReturn(null);

    // When,Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      sendNotificationRetrieverService.deleteSendNotification(sendNotificationId, organizationId, loggedUser, accessToken));
  }

  @Test
  void givenInvalidUserWhenDeleteSendNotificationThenAuthorizationDeniedException() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(-1L, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";

    // When, Then
    Assertions.assertThrows(AuthorizationDeniedException.class, () ->
      sendNotificationRetrieverService.deleteSendNotification(sendNotificationId, organizationId, loggedUser, accessToken));
  }

  @Test
  void givenValidUserWhenGetSendNotificationThenOk() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";
    SendNotificationDTO expectedResult = new SendNotificationDTO();
    expectedResult.setOrganizationId(organizationId);

    Mockito.when(notificationServiceMock.getSendNotification(sendNotificationId, accessToken)).thenReturn(
      expectedResult);

    // When
    SendNotificationDTO result = sendNotificationRetrieverService.getSendNotification(
      sendNotificationId, organizationId, loggedUser, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void givenInvalidOrganizationWhenGetSendNotificationThenThrowIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";
    SendNotificationDTO expectedResult = new SendNotificationDTO();
    expectedResult.setOrganizationId(2L);

    Mockito.when(notificationServiceMock.getSendNotification(sendNotificationId, accessToken)).thenReturn(
      expectedResult);

    // When, Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      sendNotificationRetrieverService.getSendNotification(sendNotificationId, organizationId, loggedUser, accessToken)
    );
  }

  @Test
  void givenNotFoundNotificationWhenGetSendNotificationThenThrowIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";

    Mockito.when(
      notificationServiceMock.getSendNotification(sendNotificationId, accessToken)
    ).thenReturn(null);

    // When, Then
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      sendNotificationRetrieverService.getSendNotification(sendNotificationId, organizationId, loggedUser, accessToken)
    );
  }

  @Test
  void givenInvalidUserWhenGetSendNotificationThenAuthorizationDeniedException() {
    // Given
    Long organizationId = 1L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(-1L, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "sendNotificationId";

    // When, Then
    Assertions.assertThrows(AuthorizationDeniedException.class, () ->
      sendNotificationRetrieverService.getSendNotification(sendNotificationId, organizationId, loggedUser, accessToken));
  }

  @Test
  void givenValidUserWhenGetLegalFactsThenOk() {
    // Given
    Long organizationId = 1L;
    UserInfo validUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    List<LegalFactListElementDTO> expectedResult = List.of(new LegalFactListElementDTO());

    SendNotificationDTO sendNotificationForSameOrg = new SendNotificationDTO();
    sendNotificationForSameOrg.setOrganizationId(organizationId);

    Mockito.when(
      notificationServiceMock.getSendNotification(
        sendNotificationId,
        accessToken
      )
    ).thenReturn(sendNotificationForSameOrg);

    Mockito.when(
      legalFactServiceMock.getLegalFacts(
        sendNotificationId,
        accessToken
      )
    ).thenReturn(expectedResult);

    // When
    List<LegalFactListElementDTO> actualResult =
      sendNotificationRetrieverService.getLegalFacts(
        sendNotificationId,
        organizationId,
        validUser,
        accessToken
      );

    // Then
    Assertions.assertSame(expectedResult, actualResult);
  }

  @Test
  void givenInvalidOrganizationWhenGetLegalFactsThenThrowIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo validUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";

    SendNotificationDTO sendNotificationForDifferentOrg = new SendNotificationDTO();
    sendNotificationForDifferentOrg.setOrganizationId(-1L);

    Mockito.when(
      notificationServiceMock.getSendNotification(
        sendNotificationId,
        accessToken
      )
    ).thenReturn(sendNotificationForDifferentOrg);

    // When, Then
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> sendNotificationRetrieverService.getLegalFacts(
        sendNotificationId,
        organizationId,
        validUser,
        accessToken
      )
    );
  }

  @Test
  void givenNotFoundNotificationWhenGetLegalFactsThenThrowIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo validUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";

    Mockito.when(
      notificationServiceMock.getSendNotification(
        sendNotificationId,
        accessToken
      )
    ).thenReturn(null);

    // When, Then
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> sendNotificationRetrieverService.getLegalFacts(
        sendNotificationId,
        organizationId,
        validUser,
        accessToken
      )
    );
  }

  @Test
  void givenInvalidUserWhenGetLegalFactsThenAuthorizationDeniedException() {
    // Given
    Long organizationId = 1L;
    UserInfo invalidUser = AuthorizationServiceTest.buildAdminUser(-1L, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";

    // When, Then
    Assertions.assertThrows(
      AuthorizationDeniedException.class,
      () -> sendNotificationRetrieverService.getLegalFacts(
        sendNotificationId,
        organizationId,
        invalidUser,
        accessToken
      )
    );
  }

  @Test
  void givenValidUserWhenGetLegalFactDownloadMetadataThenOk() {
    // Given
    Long organizationId = 1L;
    UserInfo validUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";
    LegalFactDownloadMetadataDTO expectedResult = new LegalFactDownloadMetadataDTO();

    SendNotificationDTO sendNotificationForSameOrg = new SendNotificationDTO();
    sendNotificationForSameOrg.setOrganizationId(organizationId);

    Mockito.when(
      notificationServiceMock.getSendNotification(
        sendNotificationId,
        accessToken
      )
    ).thenReturn(sendNotificationForSameOrg);

    Mockito.when(
      legalFactServiceMock.getLegalFactDownloadMetadata(
        sendNotificationId,
        legalFactId,
        accessToken
      )
    ).thenReturn(expectedResult);

    // When
    LegalFactDownloadMetadataDTO actualResult =
      sendNotificationRetrieverService.getLegalFactDownloadMetadata(
        sendNotificationId,
        legalFactId,
        organizationId,
        validUser,
        accessToken
      );

    // Then
    Assertions.assertSame(expectedResult, actualResult);
  }

  @Test
  void givenInvalidOrganizationWhenGetLegalFactDownloadMetadataThenThrowIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo validUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";

    SendNotificationDTO sendNotificationForDifferentOrg = new SendNotificationDTO();
    sendNotificationForDifferentOrg.setOrganizationId(-1L);

    Mockito.when(
      notificationServiceMock.getSendNotification(
        sendNotificationId,
        accessToken
      )
    ).thenReturn(sendNotificationForDifferentOrg);

    // When, Then
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> sendNotificationRetrieverService.getLegalFactDownloadMetadata(
        sendNotificationId,
        legalFactId,
        organizationId,
        validUser,
        accessToken
      )
    );
  }

  @Test
  void givenNotFoundNotificationWhenGetLegalFactDownloadMetadataThenThrowIllegalArgumentException() {
    // Given
    Long organizationId = 1L;
    UserInfo validUser = AuthorizationServiceTest.buildAdminUser(organizationId, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";

    Mockito.when(
      notificationServiceMock.getSendNotification(
        sendNotificationId,
        accessToken
      )
    ).thenReturn(null);

    // When, Then
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> sendNotificationRetrieverService.getLegalFactDownloadMetadata(
        sendNotificationId,
        legalFactId,
        organizationId,
        validUser,
        accessToken
      )
    );
  }

  @Test
  void givenInvalidUserWhenGetLegalFactDownloadMetadataThenAuthorizationDeniedException() {
    // Given
    Long organizationId = 1L;
    UserInfo invalidUser = AuthorizationServiceTest.buildAdminUser(-1L, "ORGFC", "orgIpaCode");
    String accessToken = "TOKEN";
    String sendNotificationId = "SEND_NOTIFICATION_ID";
    String legalFactId = "LEGAL_FACT_ID";

    // When, Then
    Assertions.assertThrows(
      AuthorizationDeniedException.class,
      () -> sendNotificationRetrieverService.getLegalFactDownloadMetadata(
        sendNotificationId,
        legalFactId,
        organizationId,
        invalidUser,
        accessToken
      )
    );
  }
}
