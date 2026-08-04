package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;

public class PaymentNotNotifiedException extends BaseBusinessException {

  public PaymentNotNotifiedException(String message) {
    super(ErrorCodeConstants.ERROR_CODE_PAYMENT_NOT_NOTIFIED, message);
  }

}

