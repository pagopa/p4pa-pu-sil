package it.gov.pagopa.pu.sil.exception;

public class PaymentInvalidStatusException extends RuntimeException {
  public PaymentInvalidStatusException(String message) {
    super(message);
  }
}
