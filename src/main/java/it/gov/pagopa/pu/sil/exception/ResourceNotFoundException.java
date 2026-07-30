package it.gov.pagopa.pu.sil.exception;

public class ResourceNotFoundException extends BaseBusinessException {
  public ResourceNotFoundException(String code, String message) {
    super(code, message);
  }
}

