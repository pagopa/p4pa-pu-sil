package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;

public class InvalidAccessTokenException extends BaseBusinessException {

  public InvalidAccessTokenException(String message) {
    super(ErrorCodeConstants.ERROR_CODE_INVALID_ACCESS_TOKEN, message);
  }

}
