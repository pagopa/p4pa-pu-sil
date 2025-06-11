package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.client.ExportFileClient;
import org.springframework.stereotype.Service;

@Service
public class ExportFileServiceImpl implements ExportFileService {
  private final ExportFileClient apiClient;

  public ExportFileServiceImpl(ExportFileClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public String createClassificationsExportFile(ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO, String accessToken) {
    return apiClient.createClassificationsExportFile(classificationsExportFileRequestDTO, accessToken);
  }

  @Override
  public String createPaidExportFile(PaidExportFileRequestDTO paidExportFileRequestDTO, String accessToken) {
    return apiClient.createPaidExportFile(paidExportFileRequestDTO, accessToken);
  }
}
