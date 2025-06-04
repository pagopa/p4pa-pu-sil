package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.enums.SilFaults;

public class UnauthorizedException extends RuntimeException {
  private final SilFaults code;

  public UnauthorizedException(SilFaults code, String message) {
    super(message);
    this.code = code;
  }

  public SilFaults getCode() {
    return code;
  }
}
