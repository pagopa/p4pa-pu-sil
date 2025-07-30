package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.controller.generated.PaymentApi;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.immediatepayments.InstantPaymentService;
import it.gov.pagopa.pu.sil.service.immediatepayments.VerifyNoticeService;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.Risposta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;

@Slf4j
@RestController
public class PaymentController implements PaymentApi {
  private final InstantPaymentService instantPaymentService;
  private final RegistryLogger registryLogger;
  private final VerifyNoticeService verifyNoticeService;

  public PaymentController(InstantPaymentService instantPaymentService,
                           VerifyNoticeService verifyNoticeService,
                           RegistryLogger registryLogger) {
    this.instantPaymentService = instantPaymentService;
    this.registryLogger = registryLogger;
    this.verifyNoticeService = verifyNoticeService;
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

  @Override
  public ResponseEntity<PaymentResponse> requestNoticePayment(String orgFiscalCode, String nav, String callbackUrl) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILVerificaAvviso)
      .loggedUser(userInfo)
      .build();

    return ResponseEntity.ok(
      registryLogger.execute(
        contextData,
        nav,
        () -> Triple.of(verifyNoticeService.processRequest(
          Pair.of(nav, callbackUrl),
          orgIpaCode,
          userInfo,
          accessToken
        ),
          null,
          RegistryOutcome.OK
        ),
        null
      )
    );
  }


}
