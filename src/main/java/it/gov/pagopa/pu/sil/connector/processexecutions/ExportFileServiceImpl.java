package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.ExportFileClient;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.ExportFileEntityClient;
import org.springframework.stereotype.Service;

@Service
public class ExportFileServiceImpl implements ExportFileService {
  private final ExportFileClient apiClient;
  private final ExportFileEntityClient entityClient;

  public ExportFileServiceImpl(ExportFileClient apiClient,
                               ExportFileEntityClient entityClient) {
    this.apiClient = apiClient;
    this.entityClient = entityClient;
  }

  @Override
  public Long createClassificationsExportFile(ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO, String accessToken) {
    return apiClient.createClassificationsExportFile(classificationsExportFileRequestDTO, accessToken);
  }

  @Override
  public Long createPaidExportFile(PaidExportFileRequestDTO paidExportFileRequestDTO, String accessToken) {
    return apiClient.createPaidExportFile(paidExportFileRequestDTO, accessToken);
  }

  @Override
  public ExportFile getExportFile(Long exportFileId, String accessToken) {
    return entityClient.getExportFile(exportFileId, accessToken);
  }
}
