package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.controller.generated.CheckoutApi;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import lombok.extern.slf4j.Slf4j;
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
  public ResponseEntity<Void> redirectToCheckout(String orgFiscalCode) {
    UserInfo loggedUser = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(
      loggedUser, orgFiscalCode);

    debtPositionCheckoutService.redirectToCheckout(
      loggedUser, orgIpaCode, accessToken);

    return ResponseEntity.ok().build();
  }
}
