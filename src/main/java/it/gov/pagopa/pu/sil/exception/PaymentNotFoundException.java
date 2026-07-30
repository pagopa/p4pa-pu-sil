package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;

public class PaymentNotFoundException extends BaseBusinessException {

  public PaymentNotFoundException(String message) {
    super(ErrorCodeConstants.ERROR_CODE_PAYMENT_NOT_FOUND, message);
  }

}
