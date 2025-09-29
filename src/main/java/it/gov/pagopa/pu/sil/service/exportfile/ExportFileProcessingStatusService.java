package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile.ExportFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.dto.generated.ExportStatusResponseDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class ExportFileProcessingStatusService  {
  private final ExportFileService exportFileService;
  private final String fileShareBaseUrl;

  public ExportFileProcessingStatusService(ExportFileService exportFileService,
                                           @Value("${public-base-url.fileshare}") String fileShareBaseUrl) {
    this.exportFileService = exportFileService;
    this.fileShareBaseUrl = fileShareBaseUrl;
  }

  public Pair<ExportStatusResponseDTO.StatusEnum, String> getProcessingStatus(UserInfo userInfo,
                                                                              String accessToken,
                                                                              String orgIpaCode,
                                                                              Long exportFileId,
                                                                              ExportFileTypeEnum expectedType) {
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);
    ExportFile exportFile = exportFileService.getExportFile(exportFileId, accessToken);
    log.debug("Retrieved ExportFile: {}", exportFile);

    if(exportFile == null){
      throw new IllegalArgumentException("Cannot find export file having id " + exportFileId);
    }
    if (expectedType != null && !expectedType.equals(exportFile.getExportFileType())) {
      throw new IllegalArgumentException("Type mismatch: expected %s but found %s"
        .formatted(expectedType, exportFile.getExportFileType()));
    }
    if (!ExportFileStatus.COMPLETED.equals(exportFile.getStatus())) {
      log.debug("ExportFile type {} with ID {} is not completed, returning status only: {}",
        exportFile.getExportFileType(), exportFileId, exportFile.getStatus());
      return Pair.of(ExportStatusResponseDTO.StatusEnum.fromValue(exportFile.getStatus().getValue()), null);
    } else if(exportFile.getNumTotalRows()==null || exportFile.getNumTotalRows()==0) {
      log.debug("ExportFile type {} with ID {} is completed but no data found, returning COMPLETED_NO_DATA_FOUND",
        exportFile.getExportFileType(), exportFileId);
      return Pair.of(ExportStatusResponseDTO.StatusEnum.COMPLETED_NO_DATA_FOUND, null);
    }
    return Pair.of(ExportStatusResponseDTO.StatusEnum.COMPLETED, composeUrl(exportFile));
  }

  private String composeUrl(ExportFile exportFile) {
    log.debug("Creating download URL for IngestionFlowFile: {}", exportFile);
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/exportfiles/{ingestionFlowFileId}")
      .buildAndExpand(exportFile.getOrganizationId(), exportFile.getExportFileId())
      .toUriString();
  }
}
