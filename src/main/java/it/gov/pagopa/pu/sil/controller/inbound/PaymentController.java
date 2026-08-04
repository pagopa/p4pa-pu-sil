package it.gov.pagopa.pu.sil.controller.inbound;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.controller.generated.PaymentApi;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.dto.generated.PaymentStatusResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.InstantPaymentService;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.VerifyNoticeService;
import it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.PaymentStatusRequest;
import it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.QueryPaymentsService;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentController implements PaymentApi {
  private final InstantPaymentService instantPaymentService;
  private final VerifyNoticeService verifyNoticeService;
  private final QueryPaymentsService queryPaymentsService;
  private final RegistryLogger registryLogger;
  private final String auxDigit;

  public PaymentController(InstantPaymentService instantPaymentService,
                           VerifyNoticeService verifyNoticeService,
                           QueryPaymentsService queryPaymentsService,
                           RegistryLogger registryLogger,
                           @Value("${nav.aux-digit}") String auxDigit) {
    this.instantPaymentService = instantPaymentService;
    this.verifyNoticeService = verifyNoticeService;
    this.queryPaymentsService = queryPaymentsService;
    this.registryLogger = registryLogger;
    this.auxDigit = auxDigit;
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
      .iuv(Utilities.nav2Iuv(nav, auxDigit))
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

  @Override
  public ResponseEntity<PaymentStatusResponseDTO> paymentStatus(String orgFiscalCode, QueryPaymentStatusType idType, String id, Boolean withReceiptBytes) {
    log.info("Received request for payment status for orgFiscalCode: {}, idType: {}, id: {}", orgFiscalCode, idType, id);
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    PaymentStatusRequest request = new PaymentStatusRequest(orgIpaCode, idType, id, Boolean.TRUE.equals(withReceiptBytes));

    return ResponseEntity.ok(queryPaymentsService.processRequest(
      request,
      userInfo,
      accessToken
    ));
  }
}
