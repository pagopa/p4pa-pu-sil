package it.gov.pagopa.pu.sil.service.notification;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sil.connector.send_notification.NotificationService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

  @Test
  void givenValidUserWhenCreateSendNotificationThenOk(){
    UserInfo loggedUser = new UserInfo();
    String accessToken = "TOKEN";
    Long organizationId = 1L;
    CreateNotificationRequest request = new CreateNotificationRequest();
    CreateNotificationResponse expectedResponse = new CreateNotificationResponse();
    try (MockedStatic<AuthorizationService> authorizationServiceMockedStatic = Mockito.mockStatic(AuthorizationService.class)) {
      authorizationServiceMockedStatic.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenAnswer(a->null);
      Mockito.when(notificationServiceMock.createSendNotification(organizationId, request,accessToken)).thenReturn(expectedResponse);

      CreateNotificationResponse result = sendNotificationRetrieverService.createSendNotification(
        organizationId, request, loggedUser, accessToken);

      Assertions.assertSame(expectedResponse,result);
    }
  }

  @Test
  void givenInvalidUserWhenCreateSendNotificationThenAuthorizationDeniedException() {
    UserInfo loggedUser = new UserInfo();
    String accessToken = "TOKEN";
    Long organizationId = 1L;
    CreateNotificationRequest request = new CreateNotificationRequest();

    try (MockedStatic<AuthorizationService> authorizationServiceMockedStatic = Mockito.mockStatic(AuthorizationService.class)) {
      authorizationServiceMockedStatic.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser))
        .thenThrow(new AuthorizationDeniedException("Access denied"));

      Assertions.assertThrows(AuthorizationDeniedException.class, () ->
        sendNotificationRetrieverService.createSendNotification(organizationId, request, loggedUser, accessToken));

      authorizationServiceMockedStatic.verify(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser));
      Mockito.verifyNoInteractions(notificationServiceMock);
    }
  }
}
