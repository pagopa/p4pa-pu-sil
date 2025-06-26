package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.dto.generated.PuSilErrorDTO;

public class ActualizationException extends RuntimeException {
  private final PuSilErrorDTO.CodeEnum code;

  public ActualizationException(PuSilErrorDTO.CodeEnum code, String message) {
    super(message);
    this.code = code;
  }

  public PuSilErrorDTO.CodeEnum getCode() {
    return code;
  }
}
