package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import lombok.Getter;

@Getter
public class SilFaultException extends ApplicationException {
  private final SilFaults fault;
  private final String description;

  public SilFaultException(SilFaults fault, String description) {
    super("Fault %s - %s: %s".formatted(fault.code(), fault.description(), description));
    this.fault = fault;
    this.description = description;
  }
}
