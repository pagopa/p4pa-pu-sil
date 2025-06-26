package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.sil.controller.generated.NotifyPaymentApi;
import it.gov.pagopa.pu.sil.dto.generated.NotifyPaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentNotificationController implements NotifyPaymentApi {

  @Override
  public ResponseEntity<NotifyPaymentDTO> notifyPayment(Long orgSilServiceId, String nav) {
    // TODO: this controller method will be implemented in https://pagopa.atlassian.net/browse/P4ADEV-3219
    throw new UnsupportedOperationException("This method is not implemented yet");
  }
}
