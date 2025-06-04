package it.gov.pagopa.pu.sil.exception;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) { super(message); }
}
