package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.IngestionFlowFileClient;
import org.springframework.stereotype.Service;

@Service
public class IngestionFlowFileServiceImpl implements IngestionFlowFileService {
  private final IngestionFlowFileClient apiClient;

  public IngestionFlowFileServiceImpl(IngestionFlowFileClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public Long createIngestionFlowFileReservation(IngestionFlowFileRequestDTO ingestionFlowFileDTO, String accessToken) {
    return apiClient.createIngestionFlowFileReservation(ingestionFlowFileDTO, accessToken);
  }
}
