package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import lombok.Getter;

@Getter
public class ExportFileRequestValidationException extends RuntimeException {

  private final SilFaults fault;

  public ExportFileRequestValidationException(SilFaults fault) {
    super(fault.description());
    this.fault = fault;
  }
}
