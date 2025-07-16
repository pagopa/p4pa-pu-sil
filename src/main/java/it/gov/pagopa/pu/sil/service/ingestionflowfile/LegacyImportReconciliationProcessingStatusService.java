package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LegacyImportReconciliationProcessingStatusService extends BaseIngestionFlowFileProcessingStatusService {

  public LegacyImportReconciliationProcessingStatusService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                                           IngestionFlowFileService ingestionFlowFileService) {
    super(fileShareBaseUrl, ingestionFlowFileService);
  }

  public ImportStatusResponseDTO getProcessingStatus(
      UserInfo userInfo,
      String accessToken,
      String orgIpaCode,
      Long ingestionFlowFileId,
      IngestionFlowFile.IngestionFlowFileTypeEnum... expectedTypes) {

    verifyAdminRole(userInfo, orgIpaCode);

    IngestionFlowFile ingestionFlowFile = getIngestionFlowFile(ingestionFlowFileId, accessToken);

    verifyMatchingTypes(ingestionFlowFile, expectedTypes);

    return ImportStatusResponseDTO.builder()
      .status(ingestionFlowFile.getStatus())
      .build();
  }
}
