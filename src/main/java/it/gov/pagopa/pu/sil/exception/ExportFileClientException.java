package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import lombok.Getter;

@Getter
public class ExportFileClientException extends RuntimeException {

  private final ProcessExecutionsErrorDTO.CodeEnum code;

  public ExportFileClientException(ProcessExecutionsErrorDTO.CodeEnum code, String message) {
    super(message);
    this.code = code;
  }
}
