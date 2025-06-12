package it.gov.pagopa.pu.sil.dto;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsProcessingStatusDTO {
  protected IngestionFlowFileStatus status;
  protected String urlErrors;
  protected String urlImported;
  protected String urlNotice;
}
