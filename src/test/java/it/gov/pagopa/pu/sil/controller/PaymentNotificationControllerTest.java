package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.service.paymentnotification.PaymentNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationControllerTest {
  @Mock
  private PaymentNotificationService paymentNotificationServiceMock;

  @InjectMocks
  private PaymentNotificationController controller;

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
  void whenNotifyPaymentThenOk() {
    // Given
    Long orgSilServiceId = 1L;
    String nav = "NAV123";

    doNothing().when(paymentNotificationServiceMock)
      .notifyPayment(orgSilServiceId, nav, userInfo, "fakeAccessToken");

    // When Then
    assertDoesNotThrow(() -> controller.notifyPayment(orgSilServiceId, nav));
  }
}
