package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;

public class PaymentInvalidStatusException extends BaseBusinessException {

  public PaymentInvalidStatusException(String message) {
    super(ErrorCodeConstants.ERROR_CODE_INVALID_PAYMENT_STATUS, message);
  }

}
