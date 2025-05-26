package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;

public interface IngestionFlowFileService {
  Long createIngestionFlowFileReservation(IngestionFlowFileRequestDTO ingestionFlowFileDTO, String accessToken);
}
