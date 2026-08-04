package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import lombok.Getter;

@Getter
public class SilFaultException extends BaseBusinessException {
  private final SilFaults fault;
  private final String description;

  public SilFaultException(SilFaults fault, String description) {
    super(fault.code(), "Fault %s - %s: %s".formatted(fault.code(), fault.description(), description));
    this.fault = fault;
    this.description = description;
  }

  public SilFaultException(SilFaults fault) {
    this(fault, "Fault %s - %s".formatted(fault.code(), fault.description()));
  }
}
