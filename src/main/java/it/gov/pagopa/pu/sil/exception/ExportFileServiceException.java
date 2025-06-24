package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import lombok.Getter;

@Getter
public class ExportFileServiceException extends RuntimeException {

  private final SilFaults fault;

  public ExportFileServiceException(SilFaults fault, String message) {
    super(message);
    this.fault = fault;
  }
}
