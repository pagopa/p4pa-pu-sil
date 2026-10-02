package it.gov.pagopa.pu.sil.exception.common;

import it.gov.pagopa.pu.sil.dto.generated.ErrorFieldDTO;

import java.util.List;

public class ConflictException extends BaseBusinessException {

  public ConflictException(String code, String message) {
    this(code, message, null, null, null);
  }

  public ConflictException(String code, String message, List<ErrorFieldDTO> fieldErrors) {
    this(code, message, null, fieldErrors, null);
  }

  public ConflictException(String code, String message, String silFaultCustomMessage, List<ErrorFieldDTO> fieldErrors, Throwable cause) {
    super(code, message, silFaultCustomMessage, fieldErrors, cause);
  }
}
