package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SilFaultException extends ApplicationException {
  private final SilFaults fault;
  private final String description;
}
