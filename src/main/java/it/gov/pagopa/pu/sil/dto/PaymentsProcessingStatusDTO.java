package it.gov.pagopa.pu.sil.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsProcessingStatusDTO {
  protected String status;
  protected String urlErrors;
  protected String urlImported;
  protected String urlNotice;
}
