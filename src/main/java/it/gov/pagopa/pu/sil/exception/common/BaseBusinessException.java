package it.gov.pagopa.pu.sil.exception.common;

import it.gov.pagopa.pu.sil.dto.generated.ErrorFieldDTO;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class BaseBusinessException extends RuntimeException {

  protected final String code;
  protected final List<ErrorFieldDTO> fields;
  protected final String silFaultCustomMessage;

  protected BaseBusinessException(String code, String message) {
    this(code, message, null, null, null);
  }

  protected BaseBusinessException(String code, String message, Throwable cause) {
    this(code, message, null, null, cause);
  }

  protected BaseBusinessException(String code, String message, List<ErrorFieldDTO> fields, Throwable cause) {
    this(code, message, null, fields, cause);
  }

  protected BaseBusinessException(String code, String message, String silFaultCustomMessage) {
    this(code, message, silFaultCustomMessage, null, null);
  }

  protected BaseBusinessException(String code, String message, String silFaultCustomMessage, List<ErrorFieldDTO> fields, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.fields = fields;
    this.silFaultCustomMessage = silFaultCustomMessage;
  }
}
