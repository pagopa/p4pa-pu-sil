package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse;
import it.gov.pagopa.pu.sil.service.notification.SendNotificationRetrieverService;
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
}
