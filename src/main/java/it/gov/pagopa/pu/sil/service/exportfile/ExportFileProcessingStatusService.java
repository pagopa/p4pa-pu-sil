package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile.ExportFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

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

  public Pair<ExportFileStatus, String> getProcessingStatus(UserInfo userInfo,
                                                            String accessToken,
                                                            String orgIpaCode,
                                                            Long exportFileId,
                                                            ExportFileTypeEnum expectedType) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call export file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }
    ExportFile exportFile = exportFileService.getExportFile(exportFileId, accessToken);
    log.debug("Retrieved ExportFile: {}", exportFile);

    if (!expectedType.equals(exportFile.getExportFileType())) {
      throw new IllegalArgumentException("Type mismatch: expected %s but found %s"
        .formatted(expectedType, exportFile.getExportFileType()));
    }
    if (!ExportFileStatus.COMPLETED.equals(exportFile.getStatus())) {
      log.debug("ExportFile type {} with ID {} is not completed, returning status only: {}",
        exportFile.getExportFileType(), exportFileId, exportFile.getStatus());
      return Pair.of(exportFile.getStatus(), null);
    }
    return Pair.of(exportFile.getStatus(), composeUrl(exportFile));
  }

  private String composeUrl(ExportFile exportFile) {
    log.debug("Creating download URL for IngestionFlowFile: {}", exportFile);
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/exportfiles/{ingestionFlowFileId}")
      .buildAndExpand(exportFile.getOrganizationId(), exportFile.getExportFileId())
      .toUriString();
  }
}
