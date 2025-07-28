package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.controller.generated.PaymentApi;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.immediatepayments.InstantPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentController implements PaymentApi {
  private final InstantPaymentService instantPaymentService;
  private final RegistryLogger registryLogger;

  public PaymentController(InstantPaymentService instantPaymentService, RegistryLogger registryLogger) {
    this.instantPaymentService = instantPaymentService;
    this.registryLogger = registryLogger;
  }

  @Override
  public ResponseEntity<PaymentResponse> requestInstantPayment(String orgFiscalCode,
                                                               InstantPaymentRequest instantPaymentRequest) {
    log.info("Received request for instant payment for orgFiscalCode: {}", orgFiscalCode);
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILInviaDovuti)
      .loggedUser(userInfo)
      .build();

    return ResponseEntity.ok(
      registryLogger.execute(
        contextData,
        instantPaymentRequest,
        () -> instantPaymentService.processRequest(
          instantPaymentRequest,
          orgIpaCode,
          userInfo,
          accessToken
        ),
        null
      )
    );
  }

}
