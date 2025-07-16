package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStatoImportFlusso;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LegacyImportPaymentsProcessingStatusService extends BaseIngestionFlowFileProcessingStatusService {

  public LegacyImportPaymentsProcessingStatusService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                                     IngestionFlowFileService ingestionFlowFileService) {
    super(fileShareBaseUrl, ingestionFlowFileService);
  }

  public ImportStatusResponseDTO getProcessingStatus(
      PaaSILChiediStatoImportFlusso paaSILChiediStatoImportFlusso,
      UserInfo userInfo,
      String accessToken,
      String orgIpaCode,
      Long ingestionFlowFileId,
      IngestionFlowFileTypeEnum... expectedTypes) {

    verifyAdminRole(userInfo, orgIpaCode);

    IngestionFlowFile ingestionFlowFile = getIngestionFlowFile(ingestionFlowFileId, accessToken);

    verifyMatchingTypes(ingestionFlowFile, expectedTypes);

    ImportStatusResponseDTO responseDTO = ImportStatusResponseDTO.builder()
      .status(ingestionFlowFile.getStatus())
      .build();

    if (IngestionFlowFileStatus.COMPLETED.equals(ingestionFlowFile.getStatus())) {
      addDownloadUrls(paaSILChiediStatoImportFlusso, responseDTO, ingestionFlowFile);
    }
    return responseDTO;
  }

  private void addDownloadUrls(PaaSILChiediStatoImportFlusso paaSILChiediStatoImportFlusso, ImportStatusResponseDTO responseDTO, IngestionFlowFile ingestionFlowFile) {
    if (Boolean.TRUE.equals(paaSILChiediStatoImportFlusso.isFileScarti())) {
      responseDTO.addDownloadUrlsItem(new DownloadUrl(DownloadUrl.CodeEnum.DISCARDED_FILE, composeUrl(ingestionFlowFile, "/errors")));
    }
    if (Boolean.TRUE.equals(paaSILChiediStatoImportFlusso.isFileAvvisi())) {
      responseDTO.addDownloadUrlsItem(new DownloadUrl(DownloadUrl.CodeEnum.PAYMENT_NOTICE_FILE, composeUrl(ingestionFlowFile, "/notice")));
    }
    if (Boolean.TRUE.equals(paaSILChiediStatoImportFlusso.isFileIUV())) {
      responseDTO.addDownloadUrlsItem(new DownloadUrl(DownloadUrl.CodeEnum.OUTPUT_FILE, composeUrl(ingestionFlowFile, "/imported")));
    }
  }
}
