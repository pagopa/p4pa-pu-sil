package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.dto.generated.ErrorFieldDTO;
import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;

import java.util.List;

public class WorkflowErrorException extends BaseBusinessException {
  public WorkflowErrorException(String code, String message) {
    this(code, message,  null, null, null);
  }

  public WorkflowErrorException(String code, String message, String silFaultCustomMessage) {
    this(code, message, silFaultCustomMessage, null, null);
  }

  public WorkflowErrorException(String code, String message, String silFaultCustomMessage, List<ErrorFieldDTO> fieldErrors, Throwable cause) {
    super(code, message, silFaultCustomMessage, fieldErrors, cause);
  }
}
