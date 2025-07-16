package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NativeImportProcessingStatusService extends BaseIngestionFlowFileProcessingStatusService {

  public NativeImportProcessingStatusService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                             IngestionFlowFileService ingestionFlowFileService) {
    super(fileShareBaseUrl, ingestionFlowFileService);
  }

  public ImportStatusResponseDTO getProcessingStatus(
      UserInfo userInfo,
      String accessToken,
      String orgIpaCode,
      Long ingestionFlowFileId) {

    verifyAdminRole(userInfo, orgIpaCode);

    IngestionFlowFile ingestionFlowFile = getIngestionFlowFile(ingestionFlowFileId, accessToken);

    ImportStatusResponseDTO responseDTO = ImportStatusResponseDTO.builder()
      .status(ingestionFlowFile.getStatus())
      .build();

    if (IngestionFlowFileStatus.COMPLETED.equals(ingestionFlowFile.getStatus())) {
      addDownloadUrls(responseDTO, ingestionFlowFile);
    }
    return responseDTO;
  }

  private void addDownloadUrls(ImportStatusResponseDTO responseDTO, IngestionFlowFile ingestionFlowFile) {
    responseDTO.addDownloadUrlsItem(new DownloadUrl(DownloadUrl.CodeEnum.OUTPUT_FILE, composeUrl(ingestionFlowFile, "/imported")));
    if (ingestionFlowFile.getDiscardFileName() != null) {
      responseDTO.addDownloadUrlsItem(new DownloadUrl(DownloadUrl.CodeEnum.DISCARDED_FILE, composeUrl(ingestionFlowFile, "/errors")));
    }
    if (ingestionFlowFile.getPdfGeneratedId() != null) {
      responseDTO.addDownloadUrlsItem(new DownloadUrl(DownloadUrl.CodeEnum.PAYMENT_NOTICE_FILE, composeUrl(ingestionFlowFile, "/notice")));
    }
  }
}
