package it.gov.pagopa.pu.sil.controller.outbound;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.controller.generated.NotifyPaymentApi;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.outbound.paymentnotification.PaymentNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentNotificationController implements NotifyPaymentApi {
  private final PaymentNotificationService paymentNotificationService;

  public PaymentNotificationController(PaymentNotificationService paymentNotificationService) {
    this.paymentNotificationService = paymentNotificationService;
  }

  @Override
  public ResponseEntity<Void> notifyPayment(Long orgSilServiceId, InstallmentDTO installmentDTO) {
    log.info("Requested payment notification for orgSilServiceId: {}, nav: {}", orgSilServiceId, installmentDTO.getNav());
    paymentNotificationService.notifyPayment(orgSilServiceId, installmentDTO,
      SecurityUtils.getLoggedUser(), SecurityUtils.getAccessToken());
    return ResponseEntity.ok().build();
  }
}
