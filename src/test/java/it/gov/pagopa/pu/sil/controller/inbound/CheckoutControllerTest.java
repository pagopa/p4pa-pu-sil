package it.gov.pagopa.pu.sil.controller.inbound;

import static it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionCheckoutService.CHECKOUT_RESOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import it.gov.pagopa.pu.auth.dto.generated.UserInfoLimitedScope;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionCheckoutService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

  @Mock
  private DebtPositionCheckoutService debtPositionCheckoutServiceMock;

  @InjectMocks
  private CheckoutController controller;

  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private UserInfoLimitedScope loggedUser;

  @BeforeEach
  void setUp() {
    String validResourceId = "IUV1,IUV2,IUV3";

    String orgIpaCode = "userIpaCode";
    loggedUser = AuthorizationServiceTest.buildUserLimitedScope(1L,
      orgFiscalCode, orgIpaCode, CHECKOUT_RESOURCE, validResourceId);
    loggedUser.setMappedExternalUserId("fakeExternalUser");
    SecurityUtilsTest.configureSecurityContext(accessToken, loggedUser);
  }

  @AfterEach
  void clear() {
    SecurityUtilsTest.clearSecurityContext();
  }

  @Test
  void givenValidUserInfoLimitedScopeWhenRedirectToCheckoutThenOk() {
    // Given
    String checkoutUrl = "http://www.test.com";
    when(debtPositionCheckoutServiceMock.redirectToCheckout(loggedUser, accessToken))
      .thenReturn(checkoutUrl);

    // When
    ResponseEntity<Void> response = controller.checkout(orgFiscalCode);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatusCode.valueOf(301), response.getStatusCode());
    assertEquals(checkoutUrl, response.getHeaders().getLocation().toString());
  }
}
