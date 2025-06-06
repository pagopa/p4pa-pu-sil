package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class IngestionFlowFileProcessingStatusService {
  private final IngestionFlowFileService ingestionFlowFileService;

  @Autowired
  public IngestionFlowFileProcessingStatusService(IngestionFlowFileService ingestionFlowFileService) {
    this.ingestionFlowFileService = ingestionFlowFileService;
  }

  public IngestionFlowFileStatus getProcessingStatus(UserInfo userInfo,
                                                     String accessToken,
                                                     String orgIpaCode,
                                                     Long ingestionFlowFileId,
                                                     IngestionFlowFileTypeEnum expectedType) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call ingestion flow file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }
    IngestionFlowFile ingestionFlowFile = ingestionFlowFileService.getIngestionFlowFile(ingestionFlowFileId, accessToken);
    log.debug("Retrieved IngestionFlowFile: {}", ingestionFlowFile);

    if (!ingestionFlowFile.getIngestionFlowFileType().equals(expectedType)) {
      throw new IllegalArgumentException("Type mismatch: expected %s but found %s"
        .formatted(expectedType, ingestionFlowFile.getIngestionFlowFileType()));
    }
    return ingestionFlowFile.getStatus();
  }
}

