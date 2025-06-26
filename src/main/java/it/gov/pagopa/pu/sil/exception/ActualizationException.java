package it.gov.pagopa.pu.sil.exception;

public class ActualizationException extends RuntimeException {
  private final String code;

  public ActualizationException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
