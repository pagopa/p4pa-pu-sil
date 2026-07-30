package it.gov.pagopa.pu.sil.exception;

import lombok.Getter;

@Getter
public class IngestionFlowFileTypeValidationException extends BaseBusinessException {

  private final String rejectedValue;

  public IngestionFlowFileTypeValidationException(String code, String message, String rejectedValue) {
    super(code, message);
    this.rejectedValue = rejectedValue;
  }
}
