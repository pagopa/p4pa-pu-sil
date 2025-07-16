package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
public abstract class BaseIngestionFlowFileProcessingStatusService {
  protected final String fileShareBaseUrl;
  protected final IngestionFlowFileService ingestionFlowFileService;

  protected BaseIngestionFlowFileProcessingStatusService(String fileShareBaseUrl,
                                                         IngestionFlowFileService ingestionFlowFileService) {
    this.fileShareBaseUrl = fileShareBaseUrl;
    this.ingestionFlowFileService = ingestionFlowFileService;
  }

  protected void verifyAdminRole(UserInfo userInfo, String orgIpaCode) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call ingestion flow file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }
  }

  protected IngestionFlowFile getIngestionFlowFile(Long ingestionFlowFileId, String accessToken) {
    IngestionFlowFile ingestionFlowFile = ingestionFlowFileService.getIngestionFlowFile(ingestionFlowFileId, accessToken);
    log.debug("Retrieved IngestionFlowFile: {}", ingestionFlowFile);
    return ingestionFlowFile;
  }

  protected void verifyMatchingTypes(IngestionFlowFile ingestionFlowFile, IngestionFlowFileTypeEnum... expectedTypes) {
    if (Arrays.stream(expectedTypes).noneMatch(ingestionFlowFile.getIngestionFlowFileType()::equals)) {
      throw new IllegalArgumentException("Type mismatch: expected %s but found %s"
        .formatted(expectedTypes, ingestionFlowFile.getIngestionFlowFileType()));
    }
  }

  protected String composeUrl(IngestionFlowFile ingestionFlowFile, String suffixPath) {
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/ingestionflowfiles/{ingestionFlowFileId}")
      .path(suffixPath)
      .buildAndExpand(ingestionFlowFile.getOrganizationId(), ingestionFlowFile.getIngestionFlowFileId())
      .toUriString();
  }
}
