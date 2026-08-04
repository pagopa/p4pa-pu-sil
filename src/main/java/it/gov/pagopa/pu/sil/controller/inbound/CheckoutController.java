package it.gov.pagopa.pu.sil.controller.inbound;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.controller.generated.CheckoutApi;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionCheckoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CheckoutController implements CheckoutApi {

  private final DebtPositionCheckoutService debtPositionCheckoutService;

  public CheckoutController(
    DebtPositionCheckoutService debtPositionCheckoutService) {
    this.debtPositionCheckoutService = debtPositionCheckoutService;
  }

  @Override
  public ResponseEntity<Void> checkout(String orgFiscalCode) {
    UserInfo loggedUser = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();

    log.info("Requested checkout. User traceId: [{}]",
      loggedUser != null ? loggedUser.getTraceId() : "");
    String checkoutUrl = debtPositionCheckoutService.redirectToCheckout(
      loggedUser, accessToken);

    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
      .header(HttpHeaders.LOCATION, checkoutUrl).build();
  }
}
