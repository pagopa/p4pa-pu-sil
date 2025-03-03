package it.gov.pagopa.pu.sil.exception;

public class InvalidAccessTokenException extends RuntimeException {
  public InvalidAccessTokenException(String message) {
    super(message);
  }
}
