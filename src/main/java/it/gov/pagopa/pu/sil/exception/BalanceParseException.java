package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;

public class BalanceParseException extends BaseBusinessException {

  public BalanceParseException(String message, Throwable cause) {
    super(ErrorCodeConstants.ERROR_CODE_INVALID_BALANCE, message, cause);
  }

}
