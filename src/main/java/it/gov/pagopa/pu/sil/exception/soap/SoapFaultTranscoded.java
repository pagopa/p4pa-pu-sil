package it.gov.pagopa.pu.sil.exception.soap;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SoapFaultTranscoded {
  private final SilFaults silFault;
  private final String description;
}
