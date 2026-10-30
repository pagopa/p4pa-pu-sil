package it.gov.pagopa.pu.sil.exception.common;

import it.gov.pagopa.pu.sil.dto.generated.ErrorFieldDTO;

import java.util.List;

public class NotAuthorizedException extends BaseBusinessException {
  public NotAuthorizedException(String code, String message) {
    this(code, message, null, null, null);
  }

  public NotAuthorizedException(String code, String message, String silFaultCustomMessage) {
    this(code, message, silFaultCustomMessage, null, null);
  }

  public NotAuthorizedException(String code, String message, String silFaultCustomMessage, List<ErrorFieldDTO> fieldErrors, Throwable cause) {
    super(code, message, silFaultCustomMessage, fieldErrors, cause);
  }
}
