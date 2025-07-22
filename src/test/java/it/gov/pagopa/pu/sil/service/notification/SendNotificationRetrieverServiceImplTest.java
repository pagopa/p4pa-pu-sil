package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO;
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

@ExtendWith(MockitoExtension.class)
class SendNotificationRetrieverServiceImplTest {
  @Mock
  private NotificationService notificationServiceMock;

  private SendNotificationRetrieverService sendNotificationRetrieverService;

  @BeforeEach
  void setUp() {
    sendNotificationRetrieverService = new SendNotificationRetrieverServiceImpl(notificationServiceMock);
  }

  @AfterEach
  void verifyNotMoreInteractions() {
    Mockito.verifyNoMoreInteractions(notificationServiceMock);
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
}
