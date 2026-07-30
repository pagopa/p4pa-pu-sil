package it.gov.pagopa.pu.sil.exception;

public class ConflictException extends BaseBusinessException {
  public ConflictException(String code, String message) {
    super(code, message);
  }
}
