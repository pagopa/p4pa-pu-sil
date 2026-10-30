package it.gov.pagopa.pu.sil.exception.common;

import it.gov.pagopa.pu.sil.dto.generated.ErrorFieldDTO;

import java.util.List;

public class IllegalStateBusinessException extends BaseBusinessException {
  public IllegalStateBusinessException(String code, String message) {
    this(code, message, null, null, null);
  }

  public IllegalStateBusinessException(String code, String message, Throwable cause) {
    this(code, message, null, null, cause);
  }

  public IllegalStateBusinessException(String code, String message, String silFaultCustomMessage) {
    this(code, message, silFaultCustomMessage, null, null);
  }

  public IllegalStateBusinessException(String code, String message, String silFaultCustomMessage, List<ErrorFieldDTO> fieldErrors, Throwable cause) {
    super(code, message, silFaultCustomMessage, fieldErrors, cause);
  }
}
