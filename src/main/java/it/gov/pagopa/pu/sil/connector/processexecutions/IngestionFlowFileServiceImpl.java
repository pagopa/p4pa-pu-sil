package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.IngestionFlowFileClient;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.IngestionFlowFileEntityClient;
import org.springframework.stereotype.Service;

@Service
public class IngestionFlowFileServiceImpl implements IngestionFlowFileService {
  private final IngestionFlowFileClient apiClient;
  private final IngestionFlowFileEntityClient entityClient;

  public IngestionFlowFileServiceImpl(IngestionFlowFileClient apiClient,
                                      IngestionFlowFileEntityClient entityClient) {
    this.apiClient = apiClient;
    this.entityClient = entityClient;
  }

  @Override
  public Long createIngestionFlowFileReservation(IngestionFlowFileRequestDTO ingestionFlowFileDTO, String accessToken) {
    return apiClient.createIngestionFlowFileReservation(ingestionFlowFileDTO, accessToken);
  }

  @Override
  public IngestionFlowFile getIngestionFlowFile(Long ingestionFlowFileId, String accessToken) {
    return entityClient.getIngestionFlowFile(ingestionFlowFileId, accessToken);
  }
}
