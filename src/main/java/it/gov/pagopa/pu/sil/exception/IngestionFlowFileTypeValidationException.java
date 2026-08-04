package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.Getter;

@Getter
public class IngestionFlowFileTypeValidationException extends BaseBusinessException {

  private final String rejectedValue;

  public IngestionFlowFileTypeValidationException(String message, String rejectedValue) {
    super(ErrorCodeConstants.ERROR_CODE_INVALID_INGESTION_FLOW_FILE_LEGACY_TYPE, message);
    this.rejectedValue = rejectedValue;
  }
}
