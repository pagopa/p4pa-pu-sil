package it.gov.pagopa.pu.sil.connector.processexecutions;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;

public interface ExportFileService {

  String createClassificationsExportFile(ClassificationsExportFileRequestDTO classificationsExportFileRequestDTO, String accessToken);

  String createPaidExportFile(PaidExportFileRequestDTO paidExportFileRequestDTO, String accessToken);
}
